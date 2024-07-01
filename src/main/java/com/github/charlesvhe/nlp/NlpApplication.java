package com.github.charlesvhe.nlp;

import java.util.List;

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

	/**
	 * 脱敏
	 * @param text
	 * @return
	 */
	@PostMapping("/ds")
	public String desensitization(@RequestBody String text) {
		return text;
	}

	@PostMapping("/debug")
	public List<List<Term>> debug(@RequestBody String text) {
		List<List<Term>> seg2sentence = segment.seg2sentence(text);
		return seg2sentence;
	}

	/**
	 * 添加词典
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
	public void init(){
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
