package com.pl.model.wx;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.poi.ss.formula.functions.T;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HuangGuangLei
 * @Date 2019/9/28
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class LabelVo {
    private String titleId;
    private String titleName;
    private String imageUrl;
    private Integer showStyle;
    private List<LabelVo> labelVoList;

    public String getTitleId() {
        return titleId;
    }

    public void setTitleId(String titleId) {
        this.titleId = titleId;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getShowStyle() {
        return showStyle;
    }

    public void setShowStyle(Integer showStyle) {
        this.showStyle = showStyle;
    }

    public List<LabelVo> getLabelVoList() {
        return labelVoList;
    }

    public void setLabelVoList(List<LabelVo> labelVoList) {
        this.labelVoList = labelVoList;
    }
}
