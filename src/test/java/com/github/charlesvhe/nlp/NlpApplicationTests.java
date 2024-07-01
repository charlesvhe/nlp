package com.github.charlesvhe.nlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class NlpApplicationTests {
	@Resource
	private NlpApplication nlpApplication;
	@Test
	void contextLoads() {
	}

	@Test
	public void testdesensitization(){
		String testText = "我叫吴俊泽，13036122675。" +
				"我的邮箱是w.vnfz@cgqrkqj.dz。" +
				"我家住在湖北省武汉市洪山区";
		String desensitizationText = nlpApplication.desensitization(testText);
		System.out.println(desensitizationText);
	}

}
