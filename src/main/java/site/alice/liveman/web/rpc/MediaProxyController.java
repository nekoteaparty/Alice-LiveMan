package site.alice.liveman.web.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import site.alice.liveman.mediaproxy.MediaProxyManager;

@Controller
@RequestMapping("/mediaProxy")
public class MediaProxyController {

    private static final Logger logger = LoggerFactory.getLogger(MediaProxyController.class);

    @RequestMapping("/{proxyName}/{videoId}")
    @ResponseBody
    public Object mediaProxyHandler(@PathVariable String proxyName, @PathVariable String videoId) {
        try {
            return MediaProxyManager.getMediaProxy(proxyName).requestHandler(videoId);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

}
