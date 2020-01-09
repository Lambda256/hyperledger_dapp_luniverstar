package com.lambda256.hledger.dapp;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public class ChaincodeExecuter {
    private static final Log logger = LogFactory.getLog(ChaincodeExecuter.class);

    private String chaincodeName;
    private String version;
    private ChaincodeID ccId;

    private long waitTime = 10000;

    public ChaincodeExecuter(String chaincodeName, String version) {
        this.chaincodeName = chaincodeName;
        this.version = version;

        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .setVersion(version);
        ccId = chaincodeIDBuilder.build();
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    public void setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public void executeTransaction(HFClient client, Channel channel, boolean invoke, String func, String... args) throws InvalidArgumentException, ProposalException, UnsupportedEncodingException, InterruptedException, ExecutionException, TimeoutException {
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(ccId);
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);

        transactionProposalRequest.setFcn(func);
        transactionProposalRequest.setArgs(args);
        transactionProposalRequest.setProposalWaitTime(waitTime);


        List<ProposalResponse> successful = new LinkedList<ProposalResponse>();
        List<ProposalResponse> failed = new LinkedList<ProposalResponse>();

        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
        for (ProposalResponse response : transactionPropResp) {

            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                String payload = new String(response.getChaincodeActionResponsePayload());
                logger.info(String.format("Success response from peer %s => payload: %s", response.getPeer().getName(), payload));
                successful.add(response);
            } else {
                String status = response.getStatus().toString();
                String msg = response.getMessage();
                logger.warn(String.format("Failed response from peer %s => %s: %s ", response.getPeer().getName(), status, msg));
                failed.add(response);
            }
        }

        if (invoke) {
            logger.info("Sending transaction to orderers...");

            channel.sendTransaction(successful).thenApply(transactionEvent -> {
                logger.info("Orderer response: txid" + transactionEvent.getTransactionID());
                logger.info("Orderer response: block number: " + transactionEvent.getBlockEvent().getBlockNumber());
                return null;
            }).exceptionally(e -> {
                logger.error("Orderer exception happened: ", e);
                return null;
            }).get(waitTime, TimeUnit.SECONDS);
        }
    }
}
