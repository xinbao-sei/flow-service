/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.HistoricActivityInstanceQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.persistence.AbstractManager;


/**
 * @author Tom Baeyens
 */
public class HistoricActivityInstanceEntityManager extends AbstractManager {

  public void deleteHistoricActivityInstancesByProcessInstanceId(String historicProcessInstanceId) {
    if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      getDbSqlSession().delete("deleteHistoricActivityInstancesByProcessInstanceId", historicProcessInstanceId);
    }
  }
  
  public void deleteHistoricActivityInstancesByTaskId(String taskId) {
	    if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
	      getDbSqlSession().delete("deleteHistoricActivityInstancesByTaskId", taskId);
	    }
	  }
  public void deleteHistoricActivityInstanceById(String id) {
	    if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
	      getDbSqlSession().delete("deleteHistoricActivityInstanceById", id);
	    }
	  }
  /**
   * 自定义更新 historicActivityInstance
   * @param historicActivityInstance
   */
  public void updateHistoricActivityInstance(HistoricActivityInstanceEntity historicActivityInstance) {
	    getDbSqlSession().update(historicActivityInstance);
	  }
  
  public void insertHistoricActivityInstance(HistoricActivityInstanceEntity historicActivityInstance) {
    getDbSqlSession().insert(historicActivityInstance);
  }

  public HistoricActivityInstanceEntity findHistoricActivityInstance(String activityId, String processInstanceId) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("activityId", activityId);
    parameters.put("processInstanceId", processInstanceId);
  
    return (HistoricActivityInstanceEntity) getDbSqlSession().selectOne("selectHistoricActivityInstance", parameters);
  }

  public long findHistoricActivityInstanceCountByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
    return (Long) getDbSqlSession().selectOne("selectHistoricActivityInstanceCountByQueryCriteria", historicActivityInstanceQuery);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricActivityInstance> findHistoricActivityInstancesByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery, Page page) {
    return getDbSqlSession().selectList("selectHistoricActivityInstancesByQueryCriteria", historicActivityInstanceQuery, page);
  }

  @SuppressWarnings("unchecked")
  public List<HistoricActivityInstance> findHistoricActivityInstancesByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbSqlSession().selectListWithRawParameter("selectHistoricActivityInstanceByNativeQuery", parameterMap, firstResult, maxResults);
  }

  public long findHistoricActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
    return (Long) getDbSqlSession().selectOne("selectHistoricActivityInstanceCountByNativeQuery", parameterMap);
  }
}
