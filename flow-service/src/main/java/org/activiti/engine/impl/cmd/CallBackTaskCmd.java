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
package org.activiti.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;

/**
 * @author tj
 * 自定义类，处理任务恢复
 */
public class CallBackTaskCmd implements Command<Void>, Serializable {
	
	private static final long serialVersionUID = 1L;
  
	protected TaskEntity task;
	
	protected ExecutionEntity executionEntity;
	
	public CallBackTaskCmd(Task task,Execution execution) {
		this.task = (TaskEntity) task;
		this.executionEntity  = (ExecutionEntity)execution;
	}
	
	public Void execute(CommandContext commandContext) {
	  if(task == null) {
	    throw new ActivitiIllegalArgumentException("task is null");
	  }
	  
    if (task.getRevision()==0) {
    	 executionEntity.setActive(true);
    	 executionEntity.update();
      task.insert(executionEntity);

      
      // Need to to be done here, we can't make it generic for standalone tasks 
      // and tasks from a process, as the order of setting properties is
      // completely different.
      if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
          ActivitiEventBuilder.createEntityEvent(ActivitiEventType.TASK_CREATED, task));
        
        if (task.getAssignee() != null) {
	        // The assignment event is normally fired when calling setAssignee. However, this
	        // doesn't work for standalone tasks as the commandcontext is not availble.
	        Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
	            ActivitiEventBuilder.createEntityEvent(ActivitiEventType.TASK_ASSIGNED, task));
        }
      }
     
//    executionEntity.
//      task.setExecution((DelegateExecution)executionEntity);
//    task.setExecution(executionEntity);
//    
//    task.update();
    } else {
      task.update();
    }

    return null;
	}

}
