package cn.ccnu.roombooking.service.impl;

import cn.ccnu.roombooking.common.ApiException;
import cn.ccnu.roombooking.common.log.Logs;
import cn.ccnu.roombooking.dto.request.IdentitySubmitRequest;
import cn.ccnu.roombooking.dto.response.IdentityReviewResponse;
import cn.ccnu.roombooking.dto.response.StatusResponse;
import cn.ccnu.roombooking.entity.IdentityReview;
import cn.ccnu.roombooking.entity.User;
import cn.ccnu.roombooking.enums.IdentityStatus;
import cn.ccnu.roombooking.enums.NotificationType;
import cn.ccnu.roombooking.enums.ObjectType;
import cn.ccnu.roombooking.enums.ReviewStatus;
import cn.ccnu.roombooking.mapper.IdentityReviewMapper;
import cn.ccnu.roombooking.mapper.UserMapper;
import cn.ccnu.roombooking.security.AuthContext;
import cn.ccnu.roombooking.service.AuditService;
import cn.ccnu.roombooking.service.IdentityService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** {@link IdentityService} 的默认实现：提交即落审核记录，负责人处理时同步档案状态。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

  private final UserMapper users;
  private final IdentityReviewMapper reviews;
  private final AuthContext authContext;
  private final AuditService auditService;

  @Override
  @Transactional
  public StatusResponse submit(IdentitySubmitRequest request) {
    Submitter submitter = resolveSubmitter();
    User user = submitter.user();

    // 审核中或已通过时不允许重复提交，避免刷单式覆盖已审核的资料
    if (user.getIdentityStatus() == IdentityStatus.PENDING
        || user.getIdentityStatus() == IdentityStatus.APPROVED) {
      throw ApiException.conflict("IDENTITY_ALREADY_SUBMITTED", "当前身份资料已在审核或已通过");
    }
    // 学工号全局唯一：已被他人占用时直接拒绝
    users.findByEmployeeNo(request.employeeNo())
        .filter(bound -> !bound.getId().equals(user.getId()))
        .ifPresent(bound -> {
          throw ApiException.conflict("EMPLOYEE_NO_BOUND", "学工号已绑定其他账号");
        });

    user.setName(request.name());
    user.setEmployeeNo(request.employeeNo());
    user.setMemberType(request.memberType());
    user.setPhone(request.phone());
    user.setIdentityStatus(IdentityStatus.PENDING);
    if (submitter.persisted()) {
      users.updateById(user);
    } else {
      users.insert(user);
    }

    // 审核记录保存资料快照，后续用户改资料不会污染历史审核内容
    IdentityReview review = new IdentityReview();
    review.setUserId(user.getId());
    review.setNameSnapshot(request.name());
    review.setEmployeeNoSnapshot(request.employeeNo());
    review.setMemberTypeSnapshot(request.memberType());
    review.setPhoneSnapshot(request.phone());
    review.setStatus(ReviewStatus.PENDING);
    reviews.insert(review);

    log.info("[身份提交] 审核单已创建 reviewId={} userId={} 学工号={} 身份类型={} 手机={} 首次建档={}",
        review.getId(), user.getId(), request.employeeNo(), request.memberType(),
        Logs.maskPhone(request.phone()), !submitter.persisted());

    return new StatusResponse(review.getId().toString(), review.getStatus().name(), "身份资料已提交");
  }

  @Override
  public List<IdentityReviewResponse> pendingReviews() {
    authContext.manager();
    List<IdentityReview> pending = reviews.selectList(Wrappers.<IdentityReview>lambdaQuery()
        .eq(IdentityReview::getStatus, ReviewStatus.PENDING)
        .orderByAsc(IdentityReview::getSubmittedAt));
    // 一次性把涉及的提交人和审核人捞出来，避免逐条查询造成 N+1
    Map<UUID, User> userIndex = loadUsers(pending);
    log.debug("[身份审核] 待审核列表共 {} 条", pending.size());
    return pending.stream()
        .map(review -> IdentityReviewResponse.of(review,
            userIndex.get(review.getUserId()), userIndex.get(review.getReviewerId())))
        .toList();
  }

  @Override
  @Transactional
  public IdentityReviewResponse decide(UUID reviewId, boolean approved, String reason) {
    User manager = authContext.manager();
    IdentityReview review = reviews.selectById(reviewId);
    if (review == null) {
      throw ApiException.notFound("REVIEW_NOT_FOUND", "审核记录不存在");
    }
    if (review.getStatus() != ReviewStatus.PENDING) {
      throw ApiException.conflict("REVIEW_HANDLED", "审核已处理");
    }
    if (!approved && !StringUtils.hasText(reason)) {
      throw ApiException.badRequest("REASON_REQUIRED", "拒绝必须填写原因");
    }

    review.setStatus(approved ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
    review.setReason(reason);
    review.setReviewerId(manager.getId());
    review.setHandledAt(LocalDateTime.now());
    reviews.updateById(review);

    User applicant = users.selectById(review.getUserId());
    applicant.setIdentityStatus(approved ? IdentityStatus.APPROVED : IdentityStatus.REJECTED);
    users.updateById(applicant);

    auditService.notify(applicant.getId(),
        approved ? NotificationType.APPROVED : NotificationType.REJECTED,
        review.getId(),
        approved ? "身份审核通过" : "身份审核拒绝：" + reason);
    auditService.log(manager.getId(), ObjectType.REVIEW, review.getId(),
        approved ? "APPROVE" : "REJECT", "SUCCESS", reason);

    log.info("[身份审核] {} reviewId={} 申请人={} 学工号={} 审核人={} 意见={}",
        approved ? "通过" : "拒绝", review.getId(), applicant.getId(),
        review.getEmployeeNoSnapshot(), manager.getId(), reason == null ? "-" : reason);

    return IdentityReviewResponse.of(review, applicant, manager);
  }

  /**
   * 解析提交人：优先取当前登录用户；用户档案尚未落库时按 externalId 走首次提交。
   *
   * <p>返回值中的 persisted 标记表示该用户是否已存在于数据库——首次提交需要 insert，
   * 后续提交走 update。
   */
  private Submitter resolveSubmitter() {
    try {
      return new Submitter(authContext.user(), true);
    } catch (ApiException e) {
      String externalId = authContext.externalId();
      if (!StringUtils.hasText(externalId)) {
        throw ApiException.unauthorized("UNAUTHORIZED", "身份提交需要有效登录会话");
      }
      return users.findByExternalId(externalId)
          .map(user -> new Submitter(user, true))
          .orElseGet(() -> {
            User created = new User();
            created.setExternalId(externalId);
            return new Submitter(created, false);
          });
    }
  }

  /** 批量加载审核记录关联的用户，返回 id → User 索引 */
  private Map<UUID, User> loadUsers(List<IdentityReview> list) {
    List<UUID> ids = list.stream()
        .flatMap(review -> Stream.of(review.getUserId(), review.getReviewerId()))
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, User> index = new HashMap<>();
    users.selectBatchIds(ids).forEach(user -> index.put(user.getId(), user));
    return index;
  }

  /** 提交人及其档案是否已落库 */
  private record Submitter(User user, boolean persisted) {
  }
}
