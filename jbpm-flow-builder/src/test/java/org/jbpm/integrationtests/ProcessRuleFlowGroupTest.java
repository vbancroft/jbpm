package org.jbpm.integrationtests;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.WorkingMemory;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.core.rule.Package;
import org.jbpm.integrationtests.test.Person;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.test.util.AbstractBaseTest;
import org.junit.Test;

public class ProcessRuleFlowGroupTest extends AbstractBaseTest {
    
    @Test
    public void testRuleSetProcessContext() throws Exception {
        PackageBuilder builder = new PackageBuilder();
        Reader source = new StringReader(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<process xmlns=\"http://drools.org/drools-5.0/process\"\n" +
            "         xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xs:schemaLocation=\"http://drools.org/drools-5.0/process drools-processes-5.0.xsd\"\n" +
            "         type=\"RuleFlow\" name=\"flow\" id=\"org.drools.ruleset\" package-name=\"org.jbpm\" version=\"1\" >\n" +
            "\n" +
            "  <header>\n" +
            "  </header>\n" +
            "\n" +
            "  <nodes>\n" +
            "    <start id=\"1\" name=\"Start\" />\n" +
            "    <ruleSet id=\"2\" name=\"RuleSet\" ruleFlowGroup=\"MyGroup\" >\n" +
            "    </ruleSet>\n" +
            "    <end id=\"3\" name=\"End\" />\n" +
            "  </nodes>\n" +
            "\n" +
            "  <connections>\n" +
            "    <connection from=\"1\" to=\"2\" />\n" +
            "    <connection from=\"2\" to=\"3\" />\n" +
            "  </connections>\n" +
            "\n" +
            "</process>");
        Reader source2 = new StringReader(
            "package org.jbpm;\n" +
            "\n" +
            "import org.jbpm.integrationtests.test.Person;\n" +
            "import org.kie.api.runtime.process.ProcessContext;\n" +
            "\n" +
            "rule MyRule ruleflow-group \"MyGroup\" dialect \"mvel\" \n" +
            "  when\n" +
            "    Person( age > 25 )\n" +
            "  then\n" +
            "    System.out.println(drools.getContext(ProcessContext).getProcessInstance().getProcessName());\n" +
            "end");
        builder.addRuleFlow(source);
        builder.addPackageFromDrl(source2);
        Package pkg = builder.getPackage();
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage( pkg );
        WorkingMemory workingMemory = ruleBase.newStatefulSession();
        Person person = new Person();
        person.setAge(30);
        workingMemory.insert(person);
        // start process
        RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance)
            workingMemory.startProcess("org.drools.ruleset");
        assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
        workingMemory.fireAllRules();
        assertEquals(ProcessInstance.STATE_COMPLETED, processInstance.getState());
    }

}
