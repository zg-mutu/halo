package run.halo.app.timetask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import run.halo.app.service.TongJiaService;

/**
 * 铜价
 */
@Service
public class TongJiaTask {

    public final static Logger LOG = LoggerFactory.getLogger(TongJiaTask.class);
    @Autowired
    private TongJiaService tongJiaService;

    /**
     * 定时发布铜价行情文章
     */
    @Scheduled(cron = "0 50 10 ? * MON-FRI")
    public void run() {
        LOG.info("===================工作日同步铜价定时任务开启==========================");
        long start = System.currentTimeMillis();
        try {
            tongJiaService.autoPubArticle();
        } catch (Exception e) {
            LOG.info("同步铜价定时任务异常e={}", new Object[]{e});
        }
        LOG.info("===================工作日同步铜价定时任务结束，用时={}ms.==========================", new Object[]{System.currentTimeMillis() - start});
    }


    /**
     * 周末执行
     */
    @Scheduled(cron = "0 0 9 ? * 6")
    public void run2() {
        LOG.info("===================周末同步铜价定时任务开启==========================");
        long start = System.currentTimeMillis();
        try {
            tongJiaService.autoPubArticle();
        } catch (Exception e) {
            LOG.info("同步铜价定时任务异常e={}", new Object[]{e});
        }
        LOG.info("===================同步铜价定时任务结束，用时={}ms.==========================", new Object[]{System.currentTimeMillis() - start});
    }


}
