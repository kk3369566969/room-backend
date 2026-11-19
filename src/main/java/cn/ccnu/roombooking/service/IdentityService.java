package cn.ccnu.roombooking.service;

import cn.ccnu.roombooking.dto.request.IdentitySubmitRequest;
import cn.ccnu.roombooking.dto.response.IdentityReviewResponse;
import cn.ccnu.roombooking.dto.response.StatusResponse;
import java.util.List;
import java.util.UUID;

/**
 * 身份资料提交与人工审核服务。
 *
 * <p>学生 / 教职工首次使用需要提交姓名、学工号和身份类型，由负责人人工审核通过后才可预约。
 * 状态流转：未提交 → PENDING（待审核）→ APPROVED / REJECTED；被拒绝后可重新提交。
 *
 * <p>实现类：{@link cn.ccnu.roombooking.service.impl.IdentityServiceImpl}
 */
public interface IdentityService {

  /**
   * 提交或重新提交身份资料，生成一条待审核记录。
   *
   * <p>提交人取自当前登录会话；若该会话尚未建立用户档案，会在此处按 externalId 首次落库。
   * 提交成功后用户身份状态置为 PENDING，等待负责人审核。
   *
   * @param request 姓名、学工号、身份类型（学生 / 教职工）、手机号
   * @return 审核记录 ID、当前状态与提示文案，供前端跳转到「等待审核」页
   * @throws cn.ccnu.roombooking.common.ApiException 已提交待审核或已通过（IDENTITY_ALREADY_SUBMITTED）、
   *                                                 学工号已被其他账号绑定（EMPLOYEE_NO_BOUND）、
   *                                                 无有效登录会话（UNAUTHORIZED）时抛出
   */
  StatusResponse submit(IdentitySubmitRequest request);

  /**
   * 查询全部待审核的身份申请，按提交时间升序排列（先到先审）。
   *
   * @return 待审核申请列表，携带提交人与审核人信息快照
   * @throws cn.ccnu.roombooking.common.ApiException 当前用户不是负责人（MANAGER_REQUIRED）时抛出
   */
  List<IdentityReviewResponse> pendingReviews();

  /**
   * 对一条待审核的身份申请做出通过或拒绝的决定。
   *
   * <p>同时更新审核记录与用户档案的身份状态，并向提交人投递站内通知、写入负责人操作日志。
   * 审核记录只能被处理一次。
   *
   * @param reviewId 审核记录 ID
   * @param approved true 表示通过，false 表示拒绝
   * @param reason   审核意见；拒绝时必填，通过时可为 null
   * @return 处理后的审核记录视图
   * @throws cn.ccnu.roombooking.common.ApiException 审核记录不存在（REVIEW_NOT_FOUND）、
   *                                                 已被处理（REVIEW_HANDLED）、
   *                                                 拒绝未填原因（REASON_REQUIRED）时抛出
   */
  IdentityReviewResponse decide(UUID reviewId, boolean approved, String reason);
}
