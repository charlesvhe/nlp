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
		String testText = "患者信息：\n" +
				"姓名：徐秀\n" +
				"手机号码：18132376626\n" +
				"家庭住址：西藏自治区汉中市城北区\n" +
				"性别：[男/女]\n" +
				"年龄：[年龄]\n" +
				"身份证号：510000198901267391\n" +
				"住院号：58407\n" +
				"主治医师：董伟英\n" +
				"\n" +
				"主诉：\n" +
				"[患者主要不适症状和持续时间，例如“反复头痛2周”。]\n" +
				"\n" +
				"现病史：\n" +
				"[详细描述症状起始时间、发展过程、伴随症状、曾进行的治疗及效果等。]\n" +
				"\n" +
				"既往史：\n" +
				"[记录患者以往的重要疾病及治疗情况，如“高血压10年，服药控制。”]\n" +
				"\n" +
				"家族史：\n" +
				"[描述有无遗传性疾病或家庭成员类似健康问题，如“父亲有糖尿病。”]\n" +
				"\n" +
				"个人史：\n" +
				"[包括职业、生活习惯、婚育状况等，如“无吸烟饮酒习惯，已退休。”]\n" +
				"\n" +
				"体格检查：\n" +
				"[包括一般情况、生命体征、全身各系统的检查，如“体温36.8℃，心率78次/分钟，血压130/80mmhg。”]\n" +
				"\n" +
				"辅助检查：\n" +
				"[实验室检查、影像学检查、病理报告等结果，如“血常规正常，头部ct显示左侧颞叶占位性病变。”]\n" +
				"\n" +
				"初步诊断：\n" +
				"[基于上述信息给出的初步诊断，如“疑似脑肿瘤”。]\n" +
				"\n" +
				"治疗方案：\n" +
				"[提出治疗计划，如“建议行头颅mri进一步评估并安排神经外科会诊。”]\n" +
				"\n" +
				"随访计划：\n" +
				"[出院后的随访和复查安排，如“2周后门诊复查，持续监测症状变化。”]";
		String desensitizationText = nlpApplication.desensitization(testText);
		System.out.println(desensitizationText);
	}

}
