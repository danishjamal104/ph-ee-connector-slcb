package org.mifos.connector.slcb.cucumber;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.camel.Exchange;
import org.mifos.connector.slcb.dto.PaymentRequestDTO;
import org.mifos.connector.slcb.dto.Transaction;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.List;
import static com.google.common.truth.Truth.assertThat;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SLCB_CHANNEL_REQUEST;
import static org.mifos.connector.slcb.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.connector.slcb.zeebe.ZeebeVariables.*;


public class SLCBIntegrationStepDef extends BaseStepDef {

    @Given("I have a batchId: {string}, requestId: {string}, purpose: {string}")
    public void saveTheRequiredData(String batchId, String requestId, String purpose){
        BaseStepDef.batchId = batchId;
        BaseStepDef.requestId = requestId;
        BaseStepDef.purpose = purpose;
    }


    @And("I mock transactionList with two transactions each of {string} value")
    public void mockTransactionList(String amount) {
        transactionList = new ArrayList<>();
        transactionList.add(Mockito.mock(Transaction.class));
        transactionList.add(Mockito.mock(Transaction.class));

        transactionList.forEach(transaction -> {
            Mockito.when(transaction.getBatchId()).thenReturn(batchId);
            Mockito.when(transaction.getRequestId()).thenReturn(requestId);
            Mockito.when(transaction.getAmount()).thenReturn(amount);
        });

        assertThat(transactionList).isNotEmpty();
    }

    @And("camel context is not null")
    public void camelContextCheck() {
        assertThat(template).isNotNull();
    }

    @When("I call the buildPayload route")
    public void callBuildPayloadRoute() {
        exchange = template.send("direct:build-payload", exchange -> {
            exchange.setProperty(BATCH_ID, batchId);
            exchange.setProperty(REQUEST_ID, requestId);
            exchange.setProperty(PURPOSE, purpose);
            exchange.setProperty(TRANSACTION_LIST, transactionList);
        });
        assertThat(exchange).isNotNull();
    }

    @Then("the exchange should have a variable with SLCB payload")
    public void slcbPayloadExchangeVariableCheck() {
        assertThat(exchange.getProperty(SLCB_CHANNEL_REQUEST)).isNotNull();
    }

    @And("I can parse SLCB payload to DTO")
    public void parseSlcbPayload() {
        paymentRequestDTO = exchange.getProperty(SLCB_CHANNEL_REQUEST, PaymentRequestDTO.class);
        assertThat(paymentRequestDTO).isNotNull();
    }

    @And("total transaction amount is {int}")
    public void totalTransactionAmountCheck(int totalAmount) {
        assertThat(paymentRequestDTO.getTotalAmountToBePaid()).isEqualTo(totalAmount);
    }

    @And("total transaction count is {int}, failed is {int} and completed is {int}")
    public void totalTransactionCountCheck(int totalTransactionCount,
                                           int failedTransactionCount,
                                           int completedTransactionCount) {
        assertThat(exchange.getProperty(TOTAL_TRANSACTION, Integer.class)).isEqualTo(totalTransactionCount);
        assertThat(exchange.getProperty(FAILED_TRANSACTION, Integer.class)).isEqualTo(failedTransactionCount);
        assertThat(exchange.getProperty(COMPLETED_TRANSACTION, Integer.class)).isEqualTo(completedTransactionCount);
    }

}
