package com.github.charlesvhe.nlp;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.util.DesensitizedUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import jakarta.annotation.PostConstruct;


@RestController
@SpringBootApplication
public class NlpApplication {
    private Segment segment;
    private DynamicCustomDictionary dict;
    private String emailPattern;
    private String phonePattern;

    /**
     * 脱敏
     *
     * @param text
     * @return
     */
    @PostMapping("/ds")
    public String desensitization(@RequestBody String text) {

        //先通过正则表达式过滤掉电子邮箱和电话号码
        // 匹配邮箱
        emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        Pattern emailRegex = Pattern.compile(emailPattern);
        Matcher emailMatcher = emailRegex.matcher(text);
        while (emailMatcher.find()){
            //利用DesensitizedUtil对邮件进行脱敏
            text = emailMatcher.replaceAll(DesensitizedUtil.email(emailMatcher.group()));
        }

        // 匹配电话号码
        phonePattern = "\\(?(\\d{3})\\)?[-. ]?(\\d{3})[-. ]?(\\d{4})[1-9]?";
        Pattern phoneRegex = Pattern.compile(phonePattern);
        Matcher phoneMatcher = phoneRegex.matcher(text);
        while (phoneMatcher.find()){
            //利用DesensitizedUtil对电话进行脱敏
            text = phoneMatcher.replaceAll(DesensitizedUtil.mobilePhone(phoneMatcher.group()));
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
                desensitizedText.append(DesensitizedUtil.address(word.substring(0, index), word.length() - index));
            } else if (word.contains("nr")) {
                desensitizedText.append(DesensitizedUtil.chineseName(word.substring(0, index)));
            } else {
                //如果不是隐私信息，就直接拼接原文
                desensitizedText.append(term.toString(), 0, index);
            }
        }
        return desensitizedText.toString();
    }

    @PostMapping("/debug")
    public List<List<Term>> debug(@RequestBody String text) {
        List<List<Term>> seg2sentence = segment.seg2sentence(text);
        return seg2sentence;
    }

    /**
     * @return
     */
    @PostMapping("/dict")
    public String dict() {
        dict.add("null", "null");

        return "text";
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
