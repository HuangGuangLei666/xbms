package com.pl.model.wx;

import javax.xml.crypto.Data;
import java.util.Date;

/**
 * @author HuangGuangLei
 * @Date 2019/10/11
 */
public class DialogDto {
    private Integer id;
    private String calledPhone;
    private String callerPhone;
    private Date beginDate;
    private Date endDate;
    private Integer total_seconds;
    private Date createDate;
    private String simpleWord;

    public String getSimpleWord() {
        return simpleWord;
    }

    public void setSimpleWord(String simpleWord) {
        this.simpleWord = simpleWord;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCalledPhone() {
        return calledPhone;
    }

    public void setCalledPhone(String calledPhone) {
        this.calledPhone = calledPhone;
    }

    public String getCallerPhone() {
        return callerPhone;
    }

    public void setCallerPhone(String callerPhone) {
        this.callerPhone = callerPhone;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getTotal_seconds() {
        return total_seconds;
    }

    public void setTotal_seconds(Integer total_seconds) {
        this.total_seconds = total_seconds;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
