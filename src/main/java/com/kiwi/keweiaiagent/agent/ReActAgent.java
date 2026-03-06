package com.kiwi.keweiaiagent.agent;

import com.kiwi.keweiaiagent.exception.BusinessException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Reasoning and Acting 模式的代理抽象类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent{

    /**
     * 思考方法，返回是否继续执行
     * @return
     */
   public abstract boolean think();

    /**
     * 行动方法，返回行动结果
     * @return
     */
   public abstract String act();

   @Override
   public String step(){
       try {
           boolean shouldAct = think();
           if(shouldAct){
               return act();
           }
           return "思考结束，不需要行动";
       }catch (BusinessException e){
           return "执行过程中发生错误: " + e.getMessage();
       }
   }
}
