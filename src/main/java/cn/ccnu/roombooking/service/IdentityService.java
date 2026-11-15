package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.common.*;
import cn.ccnu.roombooking.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IdentityService {
  private final UserRepository users; private final IdentityReviewRepository reviews; private final AuthContext auth; private final AuditService audit;
  public IdentityService(UserRepository users, IdentityReviewRepository reviews, AuthContext auth, AuditService audit) { this.users=users; this.reviews=reviews; this.auth=auth; this.audit=audit; }
  @Transactional public IdentityReview submit(ApiDtos.IdentitySubmitRequest req) {
    User user; try { user=auth.user(); } catch (ApiException e) { if (auth.externalId()==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","身份提交需要 X-User-Id 会话标识"); user=new User(); user.setId(UUID.randomUUID()); user.setExternalId(auth.externalId()); }
    var bound = users.findByEmployeeNo(req.employeeNo());
    if (bound.isPresent() && !bound.get().getId().equals(user.getId())) throw new ApiException(HttpStatus.CONFLICT,"EMPLOYEE_NO_BOUND","学工号已绑定其他账号");
    user.setName(req.name()); user.setEmployeeNo(req.employeeNo()); user.setMemberType(req.memberType()); user.setPhone(req.phone()); user.setIdentityStatus(Enums.IdentityStatus.PENDING); user.setUpdatedAt(LocalDateTime.now()); if (user.getCreatedAt()==null) user.setCreatedAt(LocalDateTime.now()); users.save(user);
    var r=new IdentityReview(); r.setId(UUID.randomUUID()); r.setUser(user); r.setNameSnapshot(req.name()); r.setEmployeeNoSnapshot(req.employeeNo()); r.setMemberTypeSnapshot(req.memberType()); r.setPhoneSnapshot(req.phone()); r.setStatus(Enums.ReviewStatus.PENDING); r.setSubmittedAt(LocalDateTime.now()); return reviews.save(r);
  }
  public List<IdentityReview> pending() { auth.manager(); return reviews.findByStatusOrderBySubmittedAtAsc(Enums.ReviewStatus.PENDING); }
  @Transactional public IdentityReview decide(UUID id, boolean approved, String reason) {
    User manager=auth.manager(); var r=reviews.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"REVIEW_NOT_FOUND","审核记录不存在")); if (r.getStatus()!=Enums.ReviewStatus.PENDING) throw new ApiException(HttpStatus.CONFLICT,"REVIEW_HANDLED","审核已处理"); if (!approved && (reason==null||reason.isBlank())) throw new ApiException(HttpStatus.BAD_REQUEST,"REASON_REQUIRED","拒绝必须填写原因");
    r.setStatus(approved?Enums.ReviewStatus.APPROVED:Enums.ReviewStatus.REJECTED); r.setReason(reason); r.setReviewer(manager); r.setHandledAt(LocalDateTime.now()); r.getUser().setIdentityStatus(approved?Enums.IdentityStatus.APPROVED:Enums.IdentityStatus.REJECTED); users.save(r.getUser()); audit.notify(r.getUser(), approved?Enums.NotificationType.APPROVED:Enums.NotificationType.REJECTED, r.getId(), approved?"身份审核通过":"身份审核拒绝："+reason); audit.log(manager,Enums.ObjectType.REVIEW,r.getId(),approved?"APPROVE":"REJECT","SUCCESS",reason); return reviews.save(r);
  }
}
