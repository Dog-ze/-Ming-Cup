package com.example.backend.common;

/**
 * 上传文件访问地址工具类。
 *
 * <p>本项目约定：上传接口只返回相对路径（如 {@code /uploads/admin/tea/x.png}），
 * 由客户端自行拼接当前后端地址，避免把写死的 host / scheme / contextPath 存进数据库。</p>
 *
 * <p>错误处理约定：参数非法（prefix 为空白、文件名为空白、包含 {@code ..} 路径穿越，
 * 或混入了带协议（{@code ://}）的绝对地址）时统一抛出 {@link IllegalArgumentException}，
 * 由调用方（Controller）按其错误处理策略决定如何响应，而不是静默返回 null。</p>
 */
public final class UploadUrl {

    private UploadUrl() {
    }

    /**
     * 把 prefix 规范化为「以 / 开头、以 / 结尾」后与文件名拼接，返回相对访问地址。
     *
     * <p>示例：{@code relative("/uploads/admin/", "tea/x.png")} → {@code /uploads/admin/tea/x.png}。</p>
     *
     * @param prefix   相对路径前缀，例如 {@code /uploads/admin/}；缺少首尾斜杠会自动补齐
     * @param fileName 上传服务返回的文件名或相对路径，例如 {@code tea/x.png}
     * @return 以 / 开头的相对访问地址，绝不包含 host、scheme、contextPath
     * @throws IllegalArgumentException prefix 或 fileName 非法时抛出
     */
    public static String relative(String prefix, String fileName) {
        String normalizedPrefix = normalizePrefix(prefix);
        String normalizedFileName = normalizeFileName(fileName);
        return normalizedPrefix + normalizedFileName;
    }

    /**
     * 规范化前缀：去空白、统一分隔符、补齐首尾斜杠。
     */
    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("上传地址前缀不能为空");
        }
        String value = prefix.trim().replace('\\', '/');
        if (value.contains("://")) {
            throw new IllegalArgumentException("上传地址前缀不能包含协议或主机名: " + prefix);
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    /**
     * 清洗文件名：去首尾斜杠、拒绝空白、拒绝 {@code ..} 路径穿越、拒绝绝对地址。
     */
    private static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("上传文件名不能为空");
        }
        String value = fileName.trim().replace('\\', '/');
        if (value.contains("://")) {
            throw new IllegalArgumentException("上传文件名不能是带协议的绝对地址: " + fileName);
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("上传文件名不能为空");
        }
        for (String segment : value.split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("上传文件名不允许包含 .. 路径穿越: " + fileName);
            }
        }
        return value;
    }
}
