package com.ecmp.flow.vo.bpmn;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/4/10 9:51      陈飞(fly)                  新建
 * <p/>
 * *************************************************************************************************
 */
@XmlType(name = "endEvent")
public class EndEvent extends BaseFlowNode implements Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement(name = "terminateEventDefinition")
    private String terminateEventDefinition;

    public String getTerminateEventDefinition() {
        return terminateEventDefinition;
    }

    public void setTerminateEventDefinition(String terminateEventDefinition) {
        this.terminateEventDefinition = terminateEventDefinition;
    }
}
