package com.pl.indexserver.service.impl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import com.pl.indexserver.model.SpeechcraftTagDto;
import com.pl.indexserver.model.TDialogModelDto;
import com.pl.indexserver.model.TDialogStatusInfoDto;
import com.pl.indexserver.query.TDialogQuery;
import com.pl.indexserver.service.SpeechcraftTagService;
import com.pl.indexserver.service.TDialogService;
import com.pl.indexserver.untils.*;
import com.pl.mapper.*;
import com.pl.model.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TDialogServiceImpl implements TDialogService {

    private static final String EXPORT_TAG_KEY = "export_tag";
    private static final String EXPORT_TAG_VALUE = "true";
    private static final Logger logger = LoggerFactory.getLogger(TDialogServiceImpl.class);

    @Autowired
    private TDialogMapper tDialogMapper;

    @Autowired
    private CallTaskMapper callTaskMapper;

    @Autowired
    private TBusinessPropertyMapper tBusinessPropertyMapper;

    @Autowired
    private SpeechcraftTagService speechcraftTagService;

    @Autowired
    private TmUserMapper tmUserMapper;
    @Autowired
    private TDialogDetailExtMapper tDialogDetailExtMapper;
    @Autowired
    private TBusinessMapper tBusinessMapper;

    public TDialog selectByPrimaryKey(Long taskId, Long id) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectByPrimaryKey(id, postfix);
    }

    public List<TDialog> getTDialogListByMap(Long company_id, Long task_id) {
        String postfix = getTablePostfix(task_id);
        return tDialogMapper.getTDialogListByMap(company_id, task_id, postfix);
    }


    public List<TDialog> getTDialogListToStat(Long company_id, Long task_id) {
        String postfix = getTablePostfix(task_id);
        return tDialogMapper.getTDialogListToStat(company_id, task_id, postfix);
    }


    public void addDialog(List<TDialog> dialogList, Long taskId) {
        String postfix = getTablePostfix(taskId);
        tDialogMapper.addDialog(dialogList, postfix);

    }

    public List<TDialogCount> selectAllTDialogIntentionById(Long companyId, Long id) {
        String postfix = getTablePostfix(id);
        return tDialogMapper.selectAllTDialogIntentionById(companyId, id, postfix);
    }


    public List<TDialogCount> selectAllTDialogStatusById(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectAllTDialogStatusById(companyId, taskId, postfix);
    }

    public Page<TDialogModelDto> selectAllTDialogTDialogModelDto(int pageIndex, int pageNum, Map<String, Object> map) {
        Long taskId = (Long) map.get("id");
        String postfix = getTablePostfix(taskId);
        map.put("postfix", postfix);
        List<TDialogModelDto> dtoList = new ArrayList<>();
        Page<TDialogModelDto> pages = new Page<>(pageIndex, pageNum);
        Page<TDialogSelect> page = new Page<>(pageIndex, pageNum);
        List<TDialogSelect> list = tDialogMapper.selectAllTDialogTDialogModelDto(page, map);
        if (CollectionUtils.isEmpty(list)) {
            pages.setTotal(0);
            pages.setCurrent(page.getCurrent());
            pages.setSize(0);
            return pages;
        }
        Map<Integer, String> map1 = TDialogStatusInfoDto.getMap();
        // 设置任务等级
        for (TDialogSelect tdSelt : list) {
            if ("all".equals(map.get("type"))) {
                String intention_level = StringUtils.isEmpty(tdSelt.getIntentionLevel()) ? "E" : tdSelt.getIntentionLevel();
                String focus_level = StringUtils.isEmpty(tdSelt.getFocusLevel()) ? "E" : tdSelt.getFocusLevel();
                String intent_level = StringUtils.isEmpty(tdSelt.getIntentLevel()) ? "E" : tdSelt.getIntentLevel();
                String temp = intention_level.compareTo(focus_level) > 0 ? focus_level : intention_level;
                temp = temp.compareTo(intent_level) > 0 ? intent_level : temp;
                tdSelt.setIntentionLevel(temp);
            } else if ("focus".equals(map.get("type"))) {
                tdSelt.setIntentionLevel(tdSelt.getFocusLevel());
            } else if ("intent".equals(map.get("type"))) {
                tdSelt.setIntentionLevel(tdSelt.getIntentLevel());
            }
            TDialogModelDto dto = new TDialogModelDto();
            if (StringUtils.isEmpty(tdSelt.getIntentionLevel())) {
                dto.setIntentionLevel("未知意向");
            } else {
                if (tdSelt.getIntentionLevel().equals("E")) {
                    dto.setIntentionLevel("无意向");
                } else {
                    dto.setIntentionLevel(tdSelt.getIntentionLevel());
                }
            }
            dto.setFileSize(tdSelt.getFileSize());
            dto.setErrormsg(tdSelt.getErrormsg());
            dto.setId(tdSelt.getId());
            dto.setTaskId(Long.valueOf(map.get("id").toString()));
            dto.setCtName(tdSelt.getCtName());
            dto.setCtPhone(tdSelt.getCtPhone());
            dto.setIsIntention(tdSelt.getIsIntention());
            dto.setBeginDate(tdSelt.getBeginDate());
            dto.setDuration(tdSelt.getDuration());
            dto.setOutNumber(tdSelt.getOutNumber());
            dto.setCtAddress(tdSelt.getCtAddress());
            int status = tdSelt.getStatus();
            if (map1.containsKey(status)) {
                dto.setIsIntention(status);
                dto.setIsIntentionInfo(map1.get(status));
                if (status == 2) {
                    if (dto.getErrormsg().contains("send_bye")) {
                        dto.setIsIntentionInfo("已接通[系统挂机]");
                    } else {
                        dto.setIsIntentionInfo("已接通[用户挂机]");
                    }
                }
            } else {
                dto.setIsIntention(tdSelt.getStatus());
                if (tdSelt.getErrormsg() == null || tdSelt.getErrormsg() == "") {
                    dto.setIsIntentionInfo("");
                } else {
                    dto.setIsIntentionInfo(map11.get(tdSelt.getErrormsg().toString()));
                }
            }
            Map<String, Object> info = new HashMap<>();
            info.put("intention", dto.getIsIntentionInfo());
            info.put("errormsg", tdSelt.getErrormsg());
            dto.setIntentionInfo(info);
            dtoList.add(dto);
        }
        pages.setTotal(page.getTotal());
        pages.setCurrent(page.getCurrent());
        pages.setSize(page.getSize());
        pages.setRecords(dtoList);
        return pages;
    }

    //呼叫失败原因枚举
    private static Map<String, String> map11 = new HashMap<>();

    static {
        map11.put("Cause:NORMAL_CLEARING,Message:is not reachable", "无法接通");
        map11.put("Cause:USER_BUSY,Message:bus_close", "用户忙");
        map11.put("Cause:NORMAL_CLEARING,Message:redial later", "稍后再拨提示");
        map11.put("Cause:NO_USER_RESPONSE,Message:", "呼叫未应答超时");
        map11.put("Cause:NORMAL_TEMPORARY_FAILURE,Message:ringback", "呼叫线路超时");
        map11.put("Cause:NORMAL_CLEARING,Message:not a local number", "不是本地号码");
        map11.put("Cause:UNALLOCATED_NUMBER,Message:ringback", "线路不通盲区");
        map11.put("Cause:NORMAL_CLEARING,Message:not in service", "暂停服务");
        map11.put("Cause:USER_BUSY,Message:ringback", "用户忙");
        map11.put("Cause:NORMAL_CLEARING,Message:barring of incoming", "呼入限制");
        map11.put("Cause:UNALLOCATED_NUMBER,Message:bus_close", "线路不通盲区");
        map11.put("Cause:NETWORK_OUT_OF_ORDER,Message:", "网络错误");
        map11.put("Cause:USER_BUSY,Message:", "用户忙");
        map11.put("Cause:NORMAL_CLEARING,Message:line is busy", "网络忙");
        map11.put("Cause:NO_USER_RESPONSE,Message:ringback", "呼叫未应答超时");
        map11.put("Cause:NORMAL_CLEARING,Message:number change", "改号");
        map11.put("Cause:CALL_REJECTED,Message:ringback", "呼叫被拒接");
        map11.put("Cause:SERVICE_NOT_IMPLEMENTED,Message:bus_close", "系统服务未实现");
        map11.put("Cause:RECOVERY_ON_TIMER_EXPIRE,Message:ringback", "媒体超时, 异常");
        map11.put("Cause:NETWORK_OUT_OF_ORDER,Message:bus_close", "NETWORK_OUT_OF_ORDER");
        map11.put("Cause:INVALID_NUMBER_FORMAT,Message:bus_close", "号码格式错误");
        map11.put("Cause:INCOMPATIBLE_DESTINATION,Message:", "INCOMPATIBLE_DESTINATION");
        map11.put("Cause:ORIGINATOR_CANCEL,Message:bus_close", "无人接听");
        map11.put("Cause:SERVICE_NOT_IMPLEMENTED,Message:ringback", "系统服务未实现");
        map11.put("Cause:NORMAL_TEMPORARY_FAILURE,Message:bus_close", "呼叫线路超时");
        map11.put("Cause:RECOVERY_ON_TIMER_EXPIRE,Message:bus_close", "媒体超时, 异常");
        map11.put("Cause:INVALID_GATEWAY,Message:", "网关错误");
        map11.put("Cause:ORIGINATOR_CANCEL,Message:", "无人接听");
        map11.put("Cause:UNALLOCATED_NUMBER,Message:", "线路不通盲区");
        map11.put("Cause:NO_USER_RESPONSE,Message:bus_close", "呼叫未应答超时");
        map11.put("Cause:NO_ANSWER,Message:", "无人接听");
        map11.put("Cause:CALL_REJECTED,Message:bus_close", "用户拒接");
        map11.put("Cause:INVALID_NUMBER_FORMAT,Message:", "号码格式错误");
        map11.put("Cause:ORIGINATOR_CANCEL,Message:ringback", "无人接听");
        map11.put("Cause:RECOVERY_ON_TIMER_EXPIRE,Message:", "媒体超时, 异常");
        map11.put("Cause:SERVICE_NOT_IMPLEMENTED,Message:", "系统服务未实现");
        map11.put("Cause:CALL_REJECTED,Message:", "用户拒接");
        map11.put("Cause:NORMAL_TEMPORARY_FAILURE,Message:", "呼叫线路超时");
        map11.put("Cause:NORMAL_CLEARING,Message:forwarded", "呼叫转移失败");
    }

    @Override
    public void exportAllTDialogTDialogModelDto(Map<String, Object> map, HttpServletResponse response) {
        Long taskId = (Long) map.get("id");
        String postfix = getTablePostfix(taskId);
        map.put("postfix", postfix);
        List<TDialogSelect> list = tDialogMapper.selectAllTDialogTDialogModelDto(map);
        String fileName = "任务明细表";
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet(fileName);
        CellStyle style = ExportUtil.getCellStyle(wb);
        String[] headers = {"客户姓名", "联系方式", "关注点-意向等级", "打分制-意向等级", "条件型-意向等级",
                "综合意向等级", "通话时间", "通话时长", "主叫号码", "外呼情况", "地市"};
        CallTask callTask = callTaskMapper.selectByPrimaryKey(taskId);
        // 验证智库是否需要导出标签数据
        boolean exportTagFlag = checkBusinessExportTag(callTask.getBusinessId());
        List<SpeechcraftTagDto> tagDtos = new ArrayList<>();
        if (exportTagFlag) {
            // 获取所有标签
            tagDtos = speechcraftTagService.getSpeechcraftTagDtoList(callTask.getCompanyId(), "TTS");
            headers = this.rebuildHeaders(headers, tagDtos);
        }
        ExportUtil.setAllHeader(wb, sheet, headers, style);
        Map<Integer, String> taskStatusMap = TDialogStatusInfoDto.getMap();
        for (int i = 0; i < list.size(); i++) {
            TDialogSelect t = list.get(i);
            // 获取综合意向等级
            String intentionLevel = t.getIntentionLevel();
            String focusLevel = t.getFocusLevel();
            String intentLevel = t.getIntentLevel();
            String finalLevel = StringUtils.isEmpty(intentionLevel) ? null : intentionLevel;
            if (StringUtils.isEmpty(finalLevel)) {
                finalLevel = StringUtils.isEmpty(focusLevel) ? null : focusLevel;
            } else if (!StringUtils.isEmpty(focusLevel)) {
                finalLevel = finalLevel.compareTo(focusLevel) > 0 ? focusLevel : finalLevel;
            }
            if (StringUtils.isEmpty(finalLevel)) {
                finalLevel = StringUtils.isEmpty(intentLevel) ? null : intentLevel;
            } else if (!StringUtils.isEmpty(intentLevel)) {
                finalLevel = finalLevel.compareTo(intentLevel) > 0 ? intentLevel : finalLevel;
            }
            // 打分制意向等级
            intentionLevel = StringUtils.isEmpty(intentionLevel) ? "未知意向" : intentionLevel;
            intentionLevel = "E".equals(intentionLevel) ? "无意向" : intentionLevel;
            // 关注点意向等级
            focusLevel = StringUtils.isEmpty(focusLevel) ? "未知意向" : focusLevel;
            focusLevel = "E".equals(focusLevel) ? "无意向" : focusLevel;
            // 条件意向等级
            intentLevel = StringUtils.isEmpty(intentLevel) ? "未知意向" : intentLevel;
            intentLevel = "E".equals(intentLevel) ? "无意向" : intentLevel;
            // 综合意向等级
            finalLevel = StringUtils.isEmpty(finalLevel) ? "未知意向" : finalLevel;
            finalLevel = "E".equals(finalLevel) ? "无意向" : finalLevel;
            String[] cells = new String[]{
                    t.getCtName(), t.getCtPhone(), focusLevel,
                    intentionLevel, intentLevel, finalLevel,
                    ExportUtil.objectToString(t.getBeginDate()), ExportUtil.objectToString(t.getDuration()),
                    t.getOutNumber(), taskStatusMap.getOrDefault(t.getStatus(), "呼叫失败（"+map11.get(t.getErrormsg())+"）"), t.getCtAddress()
            };
            if (exportTagFlag) {
                cells = this.rebuildCells(cells, tagDtos, t.getTtsInfo());
            }
            ExportUtil.setAllCell(wb, sheet, i + 1, cells, style);
        }
        ExportUtil.exportXls(wb, fileName, response);
    }

    @Override
    public List<String> getCallTaskPhoeList(Long company_id, Long task_id) {
        String postfix = getTablePostfix(task_id);
        return tDialogMapper.getPhoeList(company_id, task_id, postfix);
    }


    public TDialog selectByTaskIdAndTelephone(Long taskId, String telephone) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectByCallTaskIdAndTelephone(taskId.toString(), telephone, postfix);
    }

    //根据任务ID查询未拨打的电话号码 , 并加入到REDIS
    public List<TDialog> selectByTaskId(Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectByTaskId(taskId, postfix);
    }


    //根据手机号码和任务ID修改状态
    public int updateStatusByPhone(Long taskId, String phone, Integer status, Long agentId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.updateStatusByPhone(taskId, phone, status, agentId, postfix);
    }

    //根据AGENTID和任务ID修改状态
    public int updateStatusByAgent(Long taskId, Long agentId, Integer status) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.updateStatusByAgent(taskId, agentId, status, postfix);
    }

    public TDialog getDialogbyPhone(Long task_id, String telephone) {
        String postfix = getTablePostfix(task_id);
        return tDialogMapper.getDialogbyPhone(task_id, telephone, postfix);
    }

    public int updateDialogStatus(Long task_id, String telephone, Integer status, Long agent_id) {
        String postfix = getTablePostfix(task_id);
        return tDialogMapper.updateDialogStatus(task_id, telephone, status, agent_id, postfix);
    }

    public int updateTDialog(TDialog tDialog, Long taskId) {
        String postfix = getTablePostfix(taskId);
        tDialog.setTablePostfix(postfix);
        return tDialogMapper.updateTDialog(tDialog);
    }


    public TDialog getDialogbyId(Long task_id, long dialogId) {
        CallTask callTask = callTaskMapper.selectByPrimaryKey(task_id);
        if (null == callTask || callTask.getCreateDate() == null) {
            throw new CustomRunTimeException(ExceptionContant.CallTaskException.CALLTASK_NOTEXIST_ERROR, "外呼任务不存在");
        }
        String patten = "_yyyyMM";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patten);
        String postfix = simpleDateFormat.format(callTask.getCreateDate());

        TDialog tDialog = tDialogMapper.selectByPrimaryKey(dialogId, postfix);
        tDialog.setTaskName(callTask.getTaskName());
        return tDialog;
    }

    @Override
    public String getTablePostfix(long taskId) {
        CallTask callTask = callTaskMapper.selectByPrimaryKey(taskId);
        if (null == callTask || callTask.getCreateDate() == null) {
            throw new CustomRunTimeException(ExceptionContant.CallTaskException.CALLTASK_NOTEXIST_ERROR, "外呼任务不存在");
        }
        String patten = "_yyyyMM";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patten);
        return simpleDateFormat.format(callTask.getCreateDate());
    }

    public List<TCallAgentSelect> selectTCallAgentSelect(Map<String, Object> map) {
        Long taskId = (Long) map.get("usedtaskid");
        CallTask callTask = callTaskMapper.selectByPrimaryKey(taskId);
        if (null == callTask || callTask.getCreateDate() == null) {
            throw new CustomRunTimeException(ExceptionContant.CallTaskException.CALLTASK_NOTEXIST_ERROR, "外呼任务不存在");
        }
        String patten = "_yyyyMM";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patten);
        String postfix = simpleDateFormat.format(callTask.getCreateDate());
        map.put("postfix", postfix);
        return tDialogMapper.selectTCallAgentSelect(map);
    }

    @Override
    public TDialogCount selectAllIsIntentionByCompanyIdAndTaskId(Long companyId, Long taskId, String type) {
        TDialogCount tDialogCount = new TDialogCount();
        String postfix = getTablePostfix(taskId);
        Long aLong = tDialogMapper.selectAllIsIntentionByCompanyIdAndTaskId(companyId, taskId, postfix, type);
        tDialogCount.setStatus(20);
        tDialogCount.setCount(Integer.valueOf(aLong + ""));
        return tDialogCount;
    }

    @Override
    public int countDialogByTaskId(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.countDialogByTaskId(companyId, taskId, postfix);
    }

    @Override
    public int countFinishTodayDialogByTaskId(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.countFinishTodayDialogByTaskId(companyId, taskId, postfix);
    }

    @Override
    public int countFinishAllDialogByTaskId(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.countFinishAllDialogByTaskId(companyId, taskId, postfix);
    }


    @Override
    public int countUndoTelephone(List<Long> taskIds, List<String> months) {
        return tDialogMapper.countUndoTelephone(taskIds, months);
    }

    @Override
    public TDialogCount selectNoIntentionByCompanyIdAndTaskId(Long companyId, Long taskId, String type) throws Exception {
        TDialogCount tDialogCount = new TDialogCount();
        String postfix = getTablePostfix(taskId);
        int aLong = tDialogMapper.selectNoIntentionByCompanyIdAndTaskId(companyId, taskId, postfix, type);
        tDialogCount.setStatus(23);
        tDialogCount.setCount(aLong);
        return tDialogCount;
    }

    @Override
    public List<TDialog> selectByQuery(TDialogQuery tDialogQuery) throws Exception {
        String postfix = getTablePostfix(tDialogQuery.getTaskId());
        tDialogQuery.setTablePostfix(postfix);
        return tDialogMapper.selectByQuery(tDialogQuery);
    }

    @Override
    public int getVisitSuccessReportFamilyPlaningCount(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectReportFamilyPlaningCountByStatus(companyId, taskId, postfix, FamilyPlanningStatus.SUCCESS.getCode());
    }

    @Override
    public int getVisitSuccessReportJiangxiFamilyPlaningCount(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectReportJiangxiFamilyPlaningCountByStatus(companyId, taskId, postfix, ReportStatisticsStatus.VISIT_SUCCESS.getCode());
    }

    @Override
    public int getVisitSuccessReportSpeechcraftStatisticsCount(Long companyId, Long taskId) {
        String postfix = getTablePostfix(taskId);
        return tDialogMapper.selectReportSpeechcraftStatisticsCountByStatus(companyId, taskId, postfix, ReportStatisticsStatus.VISIT_SUCCESS.getCode());
    }

    @Override
    public ChatPhone getPhoneByUserName(String userName) {
        ChatPhone chatPhone = new ChatPhone();
        List<String> phones = new ArrayList<>();
        try {
            List<CallTask> callTaskList = callTaskMapper.getCallTaskByCompanyIdAndUsername(userName);
//            logger.info("=======外呼任务========callTaskList="+callTaskList);
            if (CollectionUtils.isEmpty(callTaskList)) {
                return null;
            }
            for (CallTask callTask : callTaskList) {
                String postfix = getTablePostfix(callTask.getId());
                List<TDialog> tDialogList = tDialogMapper.getTDialogListByMap(callTask.getCompanyId(), callTask.getId(), postfix);
//                logger.info("=======通话详情========tDialogList="+tDialogList);
                TBusiness tBusiness = tBusinessMapper.getBusinessName(callTask.getBusinessId());
                if (CollectionUtils.isEmpty(tDialogList)) {
                    continue;
                }
                for (TDialog tDialog : tDialogList) {
                    if (tDialog.getPriority() == 999999){
                        continue;
                    }
                    String telephone = tDialog.getTelephone();
                    TDialogDetailExt tDialogDetailExt = tDialogDetailExtMapper.getDialogRecordByDialogId(tDialog.getId(), postfix);
//                    logger.info("=======通话详情========tDialogDetailExt="+tDialogDetailExt);
                    if (tDialogDetailExt != null) {
                        String detailRecords = tDialogDetailExt.getDetailRecords();
                        if (detailRecords.contains("手机号是微信结束语")){
                            phones.add(telephone);
                            chatPhone.setBussinessId(callTask.getBusinessId());
                            chatPhone.setBussinessName(tBusiness.getName());
                            chatPhone.setCallTaskId(callTask.getId());
                            chatPhone.setCallTaskName(callTask.getTaskName());
                            chatPhone.setToAddWeXinPhones(phones);
                            chatPhone.setCode(0);
                            tDialogMapper.updateDialogPriority(tDialog.getId(),postfix);
                        }
                        /*JSONArray jsonArray = JSONArray.parseArray(detailRecords);
                        for (Object o : jsonArray) {
                            JSONObject jsonObject = JSONObject.parseObject(o.toString());
                            String infoMap = jsonObject.getString("info_map");
                            JSONObject infoMapJson = JSONObject.parseObject(infoMap);
                            String workNodeName = infoMapJson.getString("workNodeName");
                            if (!StringUtils.isEmpty(workNodeName)) {
                                if ("做生意还是上班".equals(workNodeName)) {
                                    phones.add(telephone);
                                    chatPhone.setBussinessId(callTask.getBusinessId());
                                    chatPhone.setBussinessName(tBusiness.getName());
                                    chatPhone.setCallTaskId(callTask.getId());
                                    chatPhone.setCallTaskName(callTask.getTaskName());
                                    chatPhone.setToAddWeXinPhones(phones);
                                    chatPhone.setCode(0);
                                }
                            }
                        }*/
                    }
                }
            }
        } catch (Exception e) {
            logger.info("查询号码异常~~~", e);
        }
        return chatPhone;
    }

    @Override
    public List<TDialog> selectByTaskId(String phonenumber) {
        return tDialogMapper.selectDialogByTaskId(phonenumber);
    }

    @Override
    public List<TDialog> selectAnswerListByUserId(Integer userId) {
        return tDialogMapper.selectAnswerListByUserId(userId);
    }

    private boolean checkBusinessExportTag(Long businessId) {
        TBusinessProperty tBusinessProperty = tBusinessPropertyMapper.selectByBusinessIdAndType(businessId, BusinessPropertyType.BUSINESS_EXPORT_TAG.getCode());
        if (null == tBusinessProperty) {
            return false;
        }
        JSONObject propertyJson = JSON.parseObject(tBusinessProperty.getPropertyValue());
        return EXPORT_TAG_VALUE.equalsIgnoreCase(propertyJson.getString(EXPORT_TAG_KEY));
    }

    /**
     * 重建列
     *
     * @param cells
     * @param tagDtos
     * @return
     */
    private String[] rebuildCells(String[] cells, List<SpeechcraftTagDto> tagDtos, String ttsInfo) {
        if (!CollectionUtils.isEmpty(tagDtos)) {
            String[] newCells = new String[cells.length + tagDtos.size()];
            for (int j = 0; j < cells.length; j++) {
                newCells[j] = cells[j];
            }
            for (int j = 0; j < tagDtos.size(); j++) {
                String cellValue = "";
                try {
                    JSONObject tts = JSON.parseObject(ttsInfo);
                    cellValue = tts.getString(tagDtos.get(j).getTagKey());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                newCells[cells.length + j] = cellValue;
            }
            return newCells;
        }
        return cells;
    }

    /**
     * 重建表头
     *
     * @param headers
     * @param tagDtos
     * @return
     */
    private String[] rebuildHeaders(String[] headers, List<SpeechcraftTagDto> tagDtos) {
        if (!CollectionUtils.isEmpty(tagDtos)) {
            String[] newHeaders = new String[headers.length + tagDtos.size()];
            for (int i = 0; i < headers.length; i++) {
                newHeaders[i] = headers[i];
            }
            for (int i = 0; i < tagDtos.size(); i++) {
                newHeaders[headers.length + i] = tagDtos.get(i).getTagName();
            }
            return newHeaders;
        }
        return headers;
    }

    public TDialog getDialogInbyId(long dialogId) {
        return tDialogMapper.selectDialogInByPrimaryKey(dialogId);
    }


    @Override
    public List<TDialog> selectCallInByQuery(TDialogQuery tDialogQuery) throws Exception {
        return tDialogMapper.selectCallInByQuery(tDialogQuery);
    }

    public Page<TDialogModelDto> selectCallInTDialogList(int pageIndex, int pageNum, Map<String, Object> map) {

        String postfix = "";
        map.put("postfix", postfix);
        List<TDialogModelDto> dtoList = new ArrayList<>();
        Page<TDialogModelDto> pages = new Page<>(pageIndex, pageNum);
        Page<TDialogSelect> page = new Page<>(pageIndex, pageNum);
        List<TDialogSelect> list = tDialogMapper.selectCallInTDialogList(page, map);
        if (list.size() == 0) {
            pages.setTotal(0);
            pages.setCurrent(page.getCurrent());
            pages.setSize(0);
            return pages;
        }
        Map<Integer, String> map1 = TDialogStatusInfoDto.getMap();
        // 设置任务等级
        for (TDialogSelect tdSelt : list) {
            if ("all".equals(map.get("type"))) {
                String intention_level = StringUtils.isEmpty(tdSelt.getIntentionLevel()) ? "E" : tdSelt.getIntentionLevel();
                String focus_level = StringUtils.isEmpty(tdSelt.getFocusLevel()) ? "E" : tdSelt.getFocusLevel();
                String intent_level = StringUtils.isEmpty(tdSelt.getIntentLevel()) ? "E" : tdSelt.getIntentLevel();
                String temp = intention_level.compareTo(focus_level) > 0 ? focus_level : intention_level;
                temp = temp.compareTo(intent_level) > 0 ? intent_level : temp;
                tdSelt.setIntentionLevel(temp);
            } else if ("focus".equals(map.get("type"))) {
                tdSelt.setIntentionLevel(tdSelt.getFocusLevel());
            } else if ("intent".equals(map.get("type"))) {
                tdSelt.setIntentionLevel(tdSelt.getIntentLevel());
            }
            TDialogModelDto dto = new TDialogModelDto();
            if (StringUtils.isEmpty(tdSelt.getIntentionLevel())) {
                dto.setIntentionLevel("未知意向");
            } else {
                if (tdSelt.getIntentionLevel().equals("E")) {
                    dto.setIntentionLevel("无意向");
                } else {
                    dto.setIntentionLevel(tdSelt.getIntentionLevel());
                }
            }
            dto.setFileSize(tdSelt.getFileSize());
            dto.setErrormsg(tdSelt.getErrormsg());
            dto.setId(tdSelt.getId());
            dto.setTaskId(Long.valueOf(map.get("id").toString()));
            dto.setCtName(tdSelt.getCtName());
            dto.setCtPhone(tdSelt.getCtPhone());
            dto.setIsIntention(tdSelt.getIsIntention());
            dto.setBeginDate(tdSelt.getBeginDate());
            dto.setDuration(tdSelt.getDuration());
            dto.setOutNumber(tdSelt.getOutNumber());
            dto.setCtAddress(tdSelt.getCtAddress());
            if (map1.containsKey(tdSelt.getStatus())) {
                if (tdSelt.getStatus() == 2) {
                    dto.setIsIntention(2);
                    dto.setIsIntentionInfo(map1.get(tdSelt.getStatus()));
                } else {
                    dto.setIsIntention(tdSelt.getStatus());
                    dto.setIsIntentionInfo(map1.get(tdSelt.getStatus()));
                }
            } else {
                dto.setIsIntention(tdSelt.getStatus());
                dto.setIsIntentionInfo("呼叫失败");
            }
            Map<String, Object> info = new HashMap<>();
            info.put("intention", dto.getIsIntentionInfo());
            info.put("errormsg", tdSelt.getErrormsg());
            dto.setIntentionInfo(info);
            dtoList.add(dto);
        }
        pages.setTotal(page.getTotal());
        pages.setCurrent(page.getCurrent());
        pages.setSize(page.getSize());
        pages.setRecords(dtoList);
        return pages;
    }


    @Override
    public void exportAllTDialogInTDialogModelDto(Map<String, Object> map, HttpServletResponse response) {
        List<TDialogSelect> list = tDialogMapper.selectAllTDialogInTDialogModelDto(map);
        String fileName = "任务明细表";
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet(fileName);
        CellStyle style = ExportUtil.getCellStyle(wb);
        String[] headers = {"客户姓名", "联系方式", "关注点-意向等级", "打分制-意向等级", "条件型-意向等级",
                "综合意向等级", "通话时间", "通话时长", "主叫号码", "外呼情况", "地市"};
        ExportUtil.setAllHeader(wb, sheet, headers, style);
        Map<Integer, String> taskStatusMap = TDialogStatusInfoDto.getMap();
        for (int i = 0; i < list.size(); i++) {
            TDialogSelect t = list.get(i);
            // 获取综合意向等级
            String intentionLevel = t.getIntentionLevel();
            String focusLevel = t.getFocusLevel();
            String intentLevel = t.getIntentLevel();
            String finalLevel = StringUtils.isEmpty(intentionLevel) ? null : intentionLevel;
            if (StringUtils.isEmpty(finalLevel)) {
                finalLevel = StringUtils.isEmpty(focusLevel) ? null : focusLevel;
            } else if (!StringUtils.isEmpty(focusLevel)) {
                finalLevel = finalLevel.compareTo(focusLevel) > 0 ? focusLevel : finalLevel;
            }
            if (StringUtils.isEmpty(finalLevel)) {
                finalLevel = StringUtils.isEmpty(intentLevel) ? null : intentLevel;
            } else if (!StringUtils.isEmpty(intentLevel)) {
                finalLevel = finalLevel.compareTo(intentLevel) > 0 ? intentLevel : finalLevel;
            }
            // 打分制意向等级
            intentionLevel = StringUtils.isEmpty(intentionLevel) ? "未知意向" : intentionLevel;
            intentionLevel = "E".equals(intentionLevel) ? "无意向" : intentionLevel;
            // 关注点意向等级
            focusLevel = StringUtils.isEmpty(focusLevel) ? "未知意向" : focusLevel;
            focusLevel = "E".equals(focusLevel) ? "无意向" : focusLevel;
            // 条件意向等级
            intentLevel = StringUtils.isEmpty(intentLevel) ? "未知意向" : intentLevel;
            intentLevel = "E".equals(intentLevel) ? "无意向" : intentLevel;
            // 综合意向等级
            finalLevel = StringUtils.isEmpty(finalLevel) ? "未知意向" : finalLevel;
            finalLevel = "E".equals(finalLevel) ? "无意向" : finalLevel;
            String[] cells = new String[]{
                    t.getCtName(), t.getCtPhone(), focusLevel,
                    intentionLevel, intentLevel, finalLevel,
                    ExportUtil.objectToString(t.getBeginDate()), ExportUtil.objectToString(t.getDuration()),
                    t.getOutNumber(), taskStatusMap.getOrDefault(t.getStatus(), "呼叫失败"), t.getCtAddress()
            };
            ExportUtil.setAllCell(wb, sheet, i + 1, cells, style);
        }
        ExportUtil.exportXls(wb, fileName, response);
    }


    public void addDialog(List<TDialog> dialogList) {
        tDialogMapper.addDialog(dialogList);
    }

}
