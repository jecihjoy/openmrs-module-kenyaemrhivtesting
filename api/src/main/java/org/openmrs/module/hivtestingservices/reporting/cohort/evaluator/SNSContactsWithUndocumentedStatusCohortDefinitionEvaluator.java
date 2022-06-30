package org.openmrs.module.hivtestingservices.reporting.cohort.evaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.module.hivtestingservices.query.patientContact.PatientContactQueryResult;
import org.openmrs.module.hivtestingservices.query.patientContact.definition.PatientContactQuery;
import org.openmrs.module.hivtestingservices.query.patientContact.evaluator.PatientContactQueryEvaluator;
import org.openmrs.module.hivtestingservices.reporting.cohort.definition.PNSContactsWithUndocumentedStatusCohortDefinition;
import org.openmrs.module.hivtestingservices.reporting.cohort.definition.SNSContactsWithUndocumentedStatusCohortDefinition;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * Evaluator for SNS contacts with undocumented status
 */
@Handler(supports = {SNSContactsWithUndocumentedStatusCohortDefinition.class})
public class SNSContactsWithUndocumentedStatusCohortDefinitionEvaluator implements PatientContactQueryEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());
	@Autowired
	EvaluationService evaluationService;

	public PatientContactQueryResult evaluate(PatientContactQuery definition, EvaluationContext context) throws EvaluationException {

		context = ObjectUtil.nvl(context, new EvaluationContext());
		PatientContactQueryResult queryResult = new PatientContactQueryResult(definition, context);

		String qry = "select pc.id from kenyaemr_hiv_testing_patient_contact pc\n" +
				"                      left join patient p on p.patient_id = pc.patient_related_to and p.voided = 0\n" +
				"                      left join (select ht.patient_id, mid(max(concat(ht.final_test_result)),11) as hiv_status\n" +
				"                                 from kenyaemr_etl.etl_hts_test ht group by ht.patient_id\n" +
				"                                 having hiv_status in ('Negative','Positive'))ht on ht.patient_id = pc.patient_id\n" +
				"where (pc.baseline_hiv_status not in ('Positive','Negative') or pc.baseline_hiv_status is null) and pc.relationship_type =166606 and\n" +
				"date(pc.date_created) <= date(:endDate) and pc.voided = 0 and ht.patient_id is null;";

		SqlQueryBuilder builder = new SqlQueryBuilder();
		builder.append(qry);
		Date startDate = (Date)context.getParameterValue("startDate");
		Date endDate = (Date)context.getParameterValue("endDate");
		builder.addParameter("endDate", endDate);
		builder.addParameter("startDate", startDate);
		List<Integer> results = evaluationService.evaluateToList(builder, Integer.class, context);
		queryResult.getMemberIds().addAll(results);
		return queryResult;
	}

}
