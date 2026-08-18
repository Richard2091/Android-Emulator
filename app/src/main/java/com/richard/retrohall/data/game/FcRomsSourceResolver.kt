package com.richard.retrohall.data.game

/**
 * 把 FC_ROMS 仓库的 raw.githubusercontent.com 直链展开为多个下载源。
 *
 * raw 直链单 IP 高频请求容易触发 429，且在大陆地区连通性差；
 * 因此按「国内可访问性 + 限流风险」排序，逐个候选源依次尝试，首个成功即用。
 */
object FcRomsSourceResolver {
    private const val RAW_HOST = "raw.githubusercontent.com"
    private const val PAGES_HOST = "richard2091.github.io"
    private const val OWNER = "Richard2091"
    private const val REPO = "FC_ROMS"
    private const val RAW_PREFIX = "https://$RAW_HOST/$OWNER/$REPO/"

    /**
     * 返回按优先级排列的候选源：
     * 1. GitHub Pages：无 raw 的 429 限流，大陆基本可直连；
     * 2. gcore.jsdelivr：jsDelivr 的 Gcore 节点，对大陆较友好；
     * 3. cdn.statically.io：另一免费 CDN 兜底；
     * 4. fastly.jsdelivr：全球节点质量高，但大陆连通性不稳定；
     * 5. raw.githubusercontent.com：原始源，最后兜底。
     *
     * 非本仓库 raw 链接（如 Hasheous 等外部地址）原样返回，不做改写。
     */
    fun expand(url: String): List<String> {
        if (!url.startsWith(RAW_PREFIX)) return listOf(url)
        val rest = url.removePrefix(RAW_PREFIX)
        val ref = rest.substringBefore('/')
        val path = rest.substringAfter('/', "")
        if (path.isBlank() || ref.isBlank()) return listOf(url)
        return buildList {
            add("https://$PAGES_HOST/$REPO/$path")
            add("https://gcore.jsdelivr.net/gh/$OWNER/$REPO@$ref/$path")
            add("https://cdn.statically.io/gh/$OWNER/$REPO/$ref/$path")
            add("https://fastly.jsdelivr.net/gh/$OWNER/$REPO@$ref/$path")
            add(url)
        }
    }
}
