package com.github.charlesvhe.nlp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.github.charlesvhe.nlp.component.XfXhStreamClient;
import com.github.charlesvhe.nlp.config.XfXhConfig;
import com.github.charlesvhe.nlp.dto.MsgDTO;
import com.github.charlesvhe.nlp.listener.XfXhWebSocketListener;
import com.github.charlesvhe.nlp.pojo.DictItem;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;


import javax.annotation.PostConstruct;
import javax.annotation.Resource;


@RestController
@SpringBootApplication
@Slf4j
public class NlpApplication {

    @Resource
    private XfXhStreamClient xfXhStreamClient;

    @Resource
    private XfXhConfig xfXhConfig;


    public static String prompt = "你现在是一个数据分析师，接下来我会给你提供一段长文本，请将文本中的人名以格式[人名1, 人名2, ...]的样式返回给我。文本的内容如下：";

    private Segment segment;
    private DynamicCustomDictionary dict;
    private List<String> emailPatternList = Arrays.asList("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    //缓存email正则化列表
    private static List<Pattern> emailRegexList = new ArrayList<>();
    private List<String> phonePatternList = Arrays.asList("\\(?(\\d{3})\\)?[-. ]?(\\d{3})[-. ]?(\\d{4})[1-9]?");
    //缓存电话号码正则化列表
    private static List<Pattern> phoneRegexList = new ArrayList<>();


    /**
     * 脱敏
     *
     * @param text
     * @return
     */
    @PostMapping("/ds")
    public String desensitization(@RequestBody String text) {
        //先通过正则表达式过滤掉电子邮箱和电话号码
        //匹配email
        // 如果是第一次匹配，则将匹配模式缓存到emailRegexList中
        if (emailRegexList.isEmpty()){
            for (String emailPattern : emailPatternList) {
                Pattern emailRegex = Pattern.compile(emailPattern);
                emailRegexList.add(emailRegex);
            }
        }
        for (Pattern emailRegex : emailRegexList){
            Matcher emailMatcher = emailRegex.matcher(text);
            while (emailMatcher.find()) {
                //利用DesensitizedUtil对邮件进行脱敏
                text = emailMatcher.replaceAll(DesensitizedUtil.email(emailMatcher.group()));
            }
        }

        // 匹配电话号码
        //如果是第一次匹配，则将匹配模式缓存到phoneRegexList中
        if (phoneRegexList.isEmpty()){
            for (String phonePattern : phonePatternList) {
                Pattern phoneRegex = Pattern.compile(phonePattern);
                phoneRegexList.add(phoneRegex);
            }
        }
        for (Pattern phoneRegex : phoneRegexList){
            Matcher phoneMatcher = phoneRegex.matcher(text);
            while (phoneMatcher.find()) {
                //利用DesensitizedUtil对电话进行脱敏
                text = phoneMatcher.replaceAll(DesensitizedUtil.mobilePhone(phoneMatcher.group()));
            }
        }


        //对句子进行词性分类
        List<Term> termList = segment.seg(text);

        StringBuilder desensitizedText = new StringBuilder();
        for (Term term : termList) {
            String word = term.toString();
            //获取词性在字符串中的位置
            int index = term.toString().lastIndexOf('/');
            //如果句子包含地名或者人名就进行脱敏
            if (word.contains("ns")) {
                text = text.replaceAll(word.substring(0, index), DesensitizedUtil.address(word.substring(0, index), word.length() - index));
            } else if (word.contains("nr")) {
                text = text.replaceAll(word.substring(0, index), DesensitizedUtil.chineseName(word.substring(0, index)));

            }
        }
        return text;
    }


    @PostMapping("/sendQuestion")
    public String sendQuestion(@RequestBody String question) {
        // 如果是无效字符串，则不对大模型进行请求
        if (StrUtil.isBlank(question)) {
            return "无效问题，请重新输入";
        }
        // 获取连接令牌
        if (!xfXhStreamClient.operateToken(XfXhStreamClient.GET_TOKEN_STATUS)) {
            return "当前大模型连接数过多，请稍后再试";
        }
        String completePrompt = prompt + question;
//        String completePrompt = prompt;


        // 创建消息对象
        MsgDTO msgDTO = MsgDTO.createUserMsg(completePrompt);
        // 创建监听器
        XfXhWebSocketListener listener = new XfXhWebSocketListener();
        // 发送问题给大模型，生成 websocket 连接
        WebSocket webSocket = xfXhStreamClient.sendMsg(UUID.randomUUID().toString().substring(0, 10), Collections.singletonList(msgDTO), listener);
        if (webSocket == null) {
            // 归还令牌
            xfXhStreamClient.operateToken(XfXhStreamClient.BACK_TOKEN_STATUS);
            return "系统内部错误，请联系管理员";
        }
        try {
            int count = 0;
            // 为了避免死循环，设置循环次数来定义超时时长
            int maxCount = xfXhConfig.getMaxResponseTime() * 5;
            while (count <= maxCount) {
                Thread.sleep(200);
                if (listener.isWsCloseFlag()) {
                    break;
                }
                count++;
            }
            if (count > maxCount) {
                return "大模型响应超时，请联系管理员";
            }
            // 响应大模型的答案
            System.out.println(listener.getAnswer());
            String answer = listener.getAnswer().toString();
            // 首先，我们需要移除方括号
            String cleanedNamesList = answer.replaceAll("[\\[\\]]", "");

            // 接着，使用中文逗号和空格作为分隔符分割字符串
            String[] namesArray = cleanedNamesList.split("，\\s*");

            String star = "*";

            for (String name : namesArray) {
                // 创建一个正则表达式模式，用于匹配人名
                Pattern pattern = Pattern.compile(Pattern.quote(name));

                question = pattern.matcher(question).replaceAll("***");
            }
            String result = preDesensitization(question);
            return result;
        } catch (InterruptedException e) {

            log.error("错误：" + e.getMessage());
            return "系统内部错误，请联系管理员";
        } finally {
            // 关闭 websocket 连接
            webSocket.close(1000, "");
            // 归还令牌
            xfXhStreamClient.operateToken(XfXhStreamClient.BACK_TOKEN_STATUS);
        }
    }

    @PostMapping("/debug")
    public List<Term> debug(@RequestBody String text) {
        List<Term> seg2sentence = new ArrayList<>(segment.seg(text));
        return seg2sentence;
    }

    /**
     * @description: 添加新词
     * @params: dictItem
     * @date: 2024/7/3 11:17
     * @return
     */
    @PostMapping("/dict")
    public Boolean dict(@RequestBody DictItem dictItem) {
        boolean insertResult = dict.insert(dictItem.getWord(), dictItem.getNature());
        return insertResult;
    }

    /** 
     * @description: 先用正则表达式脱敏电话号码和邮件等结构化信息
     * @params: * @param: null
     * @return: 
     * @author jzwu5
     * @date: 2024/8/1 17:27
     */ 
    
    public String preDesensitization(String text) {
        //先通过正则表达式过滤掉电子邮箱和电话号码
        //匹配email
        // 如果是第一次匹配，则将匹配模式缓存到emailRegexList中
        if (emailRegexList.isEmpty()) {
            for (String emailPattern : emailPatternList) {
                Pattern emailRegex = Pattern.compile(emailPattern);
                emailRegexList.add(emailRegex);
            }
        }
        for (Pattern emailRegex : emailRegexList) {
            Matcher emailMatcher = emailRegex.matcher(text);
            while (emailMatcher.find()) {
                //利用DesensitizedUtil对邮件进行脱敏
                text = emailMatcher.replaceAll(DesensitizedUtil.email(emailMatcher.group()));
            }
        }

        // 匹配电话号码
        //如果是第一次匹配，则将匹配模式缓存到phoneRegexList中
        if (phoneRegexList.isEmpty()) {
            for (String phonePattern : phonePatternList) {
                Pattern phoneRegex = Pattern.compile(phonePattern);
                phoneRegexList.add(phoneRegex);
            }
        }
        for (Pattern phoneRegex : phoneRegexList) {
            Matcher phoneMatcher = phoneRegex.matcher(text);
            while (phoneMatcher.find()) {
                //利用DesensitizedUtil对电话进行脱敏
                text = phoneMatcher.replaceAll(DesensitizedUtil.mobilePhone(phoneMatcher.group()));
            }
        }
        return text;
    }

    /**
     * hanlp配置
     */
    @PostConstruct
    public void init() {
        dict = new DynamicCustomDictionary();
        segment = HanLP.newSegment()
                .enableIndexMode(false).enableIndexMode(2)
                .enablePartOfSpeechTagging(true)
                .enableAllNamedEntityRecognize(false)
                .enableNameRecognize(true)
                .enablePlaceRecognize(true)
                .enableCustomDictionary(dict)
                .enableCustomDictionaryForcing(true)
                .enableNumberQuantifierRecognize(false)
                .enableOffset(true)
        ;
    }

    public static void main(String[] args) {
        SpringApplication.run(NlpApplication.class, args);
    }
}
