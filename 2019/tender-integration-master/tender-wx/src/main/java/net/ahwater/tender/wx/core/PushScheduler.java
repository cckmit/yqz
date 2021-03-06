package net.ahwater.tender.wx.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import net.ahwater.tender.db.entity.BeanItem;
import net.ahwater.tender.db.entity.BeanPushLog;
import net.ahwater.tender.db.entity.BeanUser;
import net.ahwater.tender.db.mapper.ItemMapper;
import net.ahwater.tender.db.mapper.PushLogMapper;
import net.ahwater.tender.db.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Reeye on 2018/7/19 10:59
 * Nothing is true but improving yourself.
 */
@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection"})
@Slf4j
@Component
public class PushScheduler {

    @SuppressWarnings("unchecked")
    private static final List<Map<String, ScheduledFuture>> PUSH_SCHEDULED_FUTURE = new CopyOnWriteArrayList();

    @Value("${wechat.mp.server}")
    private String serverUrl;

    @Value("${wechat.mp.templateMsgId}")
    private String templateMsgId;

    private static ThreadPoolTaskScheduler schedule = new ThreadPoolTaskScheduler();

    @Autowired
    private PushLogMapper pushLogMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private WxMpService wxMpService;

    static {
        schedule.initialize();
    }

    @SuppressWarnings("WeakerAccess")
    void newOrUpdateTask(BeanUser user) {
        Map<String, ScheduledFuture> map = PUSH_SCHEDULED_FUTURE.stream()
                .filter(e -> e.containsKey(user.getUsername()))
                .findFirst()
                .orElse(new HashMap<>());
        ScheduledFuture future = map.get(user.getUsername());
        ScheduledFuture newFuture = schedule.schedule(new PushJob(user), new CronTrigger("0 0 " + user.getPushTime() + " * * ?"));
//        ScheduledFuture newFuture = schedule.schedule(new PushJob(user), new CronTrigger("0 0/5 * * * ?"));
        map.put(user.getUsername(), newFuture);
        if (future == null) {
            PUSH_SCHEDULED_FUTURE.add(map);
            log.debug("????????????????????????: UserId[{}]-{}", user.getId(), newFuture.toString());
        } else {
            future.cancel(true);
            future = newFuture;
            log.debug("????????????????????????: UserId[{}]-{}", user.getId(), future.toString());
        }
    }

    void removeTask(BeanUser user) {
        for (int i = 0; i < PUSH_SCHEDULED_FUTURE.size(); i++) {
            if (PUSH_SCHEDULED_FUTURE.get(i).containsKey(user.getUsername())) {
                ScheduledFuture future = PUSH_SCHEDULED_FUTURE.get(i).get(user.getUsername());
                if (future != null) {
                    future.cancel(true);
                    PUSH_SCHEDULED_FUTURE.remove(i);
                    log.debug("???????????????????????? UserId[{}]-{}", user.getId(), future.toString());
                    break;
                }
            }
        }
    }

    public class PushJob implements Runnable {

        private BeanUser user;

        PushJob(BeanUser user) {
            this.user = user;
        }

        @Override
        public void run() {
            log.debug("UserId:[{}]????????????????????????", user.getId());
            List<BeanPushLog> logs = pushLogMapper.selectList(new QueryWrapper<BeanPushLog>()
                    .lambda()
                    .eq(BeanPushLog::getUserId, user.getId())
                    .eq(BeanPushLog::getStatus, 1));
            log.debug("UserId:[{}] ???????????????: [{}]", user.getId(), logs.size());
            if (logs.size() > 0) {
                List<BeanItem> items = itemMapper.selectBatchIds(logs.stream()
                        .map(BeanPushLog::getItemId)
                        .collect(Collectors.toList()));
                log.debug("UserId:[{}] ?????????????????? [{}]???", user.getId(), items.size());
                if (items.size() > 0) {
                    AtomicInteger i = new AtomicInteger(0);
                    String content = items.stream()
                            .sorted(Comparator.comparingInt(BeanItem::getId))
                            .map(BeanItem::getTitle)
                            .reduce("", (pre, curr) -> {
                                if (pre.length() + curr.length() >= 150) {
                                    return pre;
                                } else {
                                    return pre + i.addAndGet(1) + "." + curr + "\n";
                                }
                            })
                            + (items.size() > i.get() ? "..." : "")
                            + "(???" + items.size() + "???)";
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                    WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                            .toUser(user.getWxopenid())
                            .templateId(templateMsgId)
                            .url(serverUrl + "/wx/index?pushCode=" + uuid)
                            .build();
                    templateMessage.getData().add(new WxMpTemplateData("first", "????????????????????????!", "#000000"));
                    templateMessage.getData().add(new WxMpTemplateData("keyword1", "???????????????????????????", "#000000"));
                    templateMessage.getData().add(new WxMpTemplateData("keyword2", DateUtils.format("yyyy-MM-dd HH:mm", new Date()), "#000000"));
                    templateMessage.getData().add(new WxMpTemplateData("remark", content, "#3388FF"));
                    try {
                        String msgId = wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
                        if (msgId != null) {
                            log.debug("????????????[ID:{}]????????????, ????????????: UserId:[{}]", msgId, user.getId());
                            List<Integer> ids = logs.stream()
                                    .map(BeanPushLog::getId)
                                    .collect(Collectors.toList());
                            BeanPushLog entity = new BeanPushLog().setStatus(2).setPushTime(new Date()).setCode(uuid);
                            int res = pushLogMapper.update(entity, new UpdateWrapper<BeanPushLog>().in("id", ids));
                            if (res == logs.size()) {
                                log.info("push_logs {}??????????????????: {}???", ids, res);
                            } else {
                                log.warn("push_logs {}?????????????????????: ?????????{}???, ????????????{}???", ids, logs.size(), res);
                            }
                        } else {
                            log.warn("????????????????????????");
                        }
                    } catch (WxErrorException e) {
                        e.printStackTrace();
                        log.error(e.getMessage());
                    }
                }
            }
        }

    }

}
