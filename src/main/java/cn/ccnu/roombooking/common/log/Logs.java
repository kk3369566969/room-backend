package cn.ccnu.roombooking.common.log;

/**
 * 日志脱敏与截断工具。
 *
 * <p>日志会长期保留并可能被多人查看，手机号、令牌、openid 这类信息不应落明文。
 * 统一走这里处理，避免各处写法不一致。
 */
public final class Logs {

  private Logs() {}

  /**
   * 手机号脱敏：保留前 3 位与后 4 位。
   *
   * @param phone 原始手机号，可为 null
   * @return 形如 138****0001；无法识别时返回 ***
   */
  public static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "***";
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  /**
   * 截断长标识（openid、会话摘要等），只保留前 8 位。
   *
   * @param value 原始值，可为 null
   * @return 截断后的值，null 转为 "-"
   */
  public static String abbrev(String value) {
    return abbrev(value, 8);
  }

  /**
   * 截断长标识，只保留前 keep 位。
   *
   * @param value 原始值，可为 null
   * @param keep  保留的字符数
   * @return 截断后的值，null 转为 "-"
   */
  public static String abbrev(String value, int keep) {
    if (value == null) {
      return "-";
    }
    return value.length() <= keep ? value : value.substring(0, keep) + "...";
  }
}
