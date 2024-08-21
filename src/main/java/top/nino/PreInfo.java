package top.nino;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : nino
 * @date : 2024/8/21 20:14
 */
public class PreInfo {
    private String slug;
    private List<String> questionList;

    public PreInfo() {
        this.questionList = new ArrayList<>();
    }

    public PreInfo(String slug, List<String> questionList) {
        this.slug = slug;
        this.questionList = questionList;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<String> getQuestionList() {
        return questionList;
    }

    public void setQuestionList(List<String> questionList) {
        this.questionList = questionList;
    }

    @Override
    public String toString() {
        return "PreInfo{" +
                "slug='" + slug + '\'' +
                ", questionList=" + questionList +
                '}';
    }
}
