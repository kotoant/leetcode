package web_crawler_multithreaded;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.function.Predicate;

/**
 * <a href="https://leetcode.com/problems/web-crawler-multithreaded/">Problem</a>
 * <p>
 * // This is the HtmlParser's API interface.
 * // You should not implement it, or speculate about its implementation
 * interface HtmlParser {
 * public List<String> getUrls(String url) {}
 * }
 */
class Solution {
    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        String hostname = extractHostnameFromUrl(startUrl);
        Predicate<String> sameHostName = sameHostname(hostname);

        ForkJoinPool forkJoinPool = new ForkJoinPool(10);
        try {
            Map<String, CrawlerRecursiveAction> actions = new ConcurrentHashMap<>();
            CrawlerRecursiveAction task = new CrawlerRecursiveAction(startUrl, htmlParser, sameHostName, actions);
            actions.put(startUrl, task);
            forkJoinPool.invoke(task);
            return actions.keySet().stream().toList();
        } finally {
            forkJoinPool.shutdown();
        }
    }

    private String extractHostnameFromUrl(String url) {
        return url.replaceFirst("http://", "").replaceFirst("/.*", "");
    }

    private Predicate<String> sameHostname(String hostname) {
        return url -> hostname.equals(extractHostnameFromUrl(url));
    }

    static class CrawlerRecursiveAction extends RecursiveAction {

        private final String startUrl;
        private final HtmlParser htmlParser;
        private final Predicate<String> sameHostname;
        private final Map<String, CrawlerRecursiveAction> actions;

        CrawlerRecursiveAction(String startUrl, HtmlParser htmlParser, Predicate<String> sameHostname, Map<String, CrawlerRecursiveAction> actions) {
            this.startUrl = startUrl;
            this.sameHostname = sameHostname;
            this.htmlParser = htmlParser;
            this.actions = actions;
        }

        @Override
        protected void compute() {
            List<String> urls = htmlParser.getUrls(startUrl);
            List<CrawlerRecursiveAction> tasks = new ArrayList<>();
            for (String url : urls) {
                if (sameHostname.test(url)) {
                    CrawlerRecursiveAction action = new CrawlerRecursiveAction(url, htmlParser, sameHostname, actions);
                    if (actions.putIfAbsent(url, action) == null) {
                        tasks.add(action);
                    }
                }
            }
            ForkJoinTask.invokeAll(tasks);
        }
    }
}


/**
 *
 **/
interface HtmlParser {
    List<String> getUrls(String url);
}