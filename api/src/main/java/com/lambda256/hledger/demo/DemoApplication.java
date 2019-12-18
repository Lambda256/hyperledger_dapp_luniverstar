package com.lambda256.hledger.demo;

import java.io.*;

import com.lambda256.hledger.dapp.ChaincodeExecuter;
import com.lambda256.hledger.dapp.FabricUser;
import org.springframework.http.HttpStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InfoException;


import static java.lang.String.format;

@SpringBootApplication
@RestController
public class DemoApplication {

	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	private static final long waitTime = 6000;
	private static String connectionProfilePath;

	private static String channelName = "mychannel";
	private static String userName = "admin";
	private static String secret = "adminpw";
	private static String chaincodeName = "myccjava";
	private static String chaincodeVersion = "1.1";

	private static Channel channel = null;
	private static HFClient client = null;

	private int fundingCount = 10;
	private int likeCount = 10;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@RequestMapping("/start")
	public ResponseEntity<String> start(@RequestParam(value = "name", defaultValue = "World") String name) {
		String response = new String();

		if (channel == null) {
			initialize();
		}

		try {
			executeChaincode(client, channel);
		} catch (Exception e) {
			logger.error("exception", e);
		}
		return new ResponseEntity<>(
				response,
				HttpStatus.OK);
	}

	@CrossOrigin(origins = "http://localhost:8080")
	@RequestMapping("/tx/{version}/histories")
	public ResponseEntity<Object> getHistories(@RequestParam(value = "next", defaultValue = "0") String next) {
		Map<String, Object> response = new HashMap<>();
/*
		if (channel == null) {
			initialize();
		}

		try {
			executeChaincode(client, channel);
		} catch (Exception e) {
			logger.error("exception", e);
		}
 */

		response.put("fundingCount", fundingCount);
		response.put("likeCount", likeCount);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@CrossOrigin(origins = "http://localhost:8080")
	@RequestMapping("/tx/{version}/transactions/{action}")
	public ResponseEntity<String> doTransaction(@RequestParam(value = "from", defaultValue = "World") String from,
										@RequestParam(value = "input", defaultValue = "null") Object input,
										@PathVariable("action") String action,
										@PathVariable("version") String version) {
		String response = new String("OK");
		logger.error(action);
/*
		if (channel == null) {
			initialize();
		}

		try {
			executeChaincode(client, channel);
		} catch (Exception e) {
			logger.error("exception", e);
		}
 */
		if (action.equals("funding")) fundingCount++;
		if (action.equals("like")) likeCount++;

		return new ResponseEntity<>(
				response,
				HttpStatus.OK);
	}

	private static void initialize() {
		connectionProfilePath = /*System.getProperty("user.dir")+ */"./connection-profile-standard.yaml";
		File f = new File(connectionProfilePath);
		try {
			javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
					new javax.net.ssl.HostnameVerifier(){

						public boolean verify(String hostname,
											  javax.net.ssl.SSLSession sslSession) {
							return hostname.equals("172.17.43.49");
						}
					});

			NetworkConfig networkConfig = NetworkConfig.fromYamlFile(f);
			NetworkConfig.OrgInfo clientOrg = networkConfig.getClientOrganization();

            /* ID/Secret Version */
            NetworkConfig.CAInfo caInfo = clientOrg.getCertificateAuthorities().get(0);
            FabricUser user = getFabricUser(clientOrg, caInfo);

			/* Cert/Key Version
			NetworkConfig.CAInfo caInfo = null;
			FabricUser user = getFabricUser4Local(clientOrg);
			*/

			client = HFClient.createNewInstance();
			client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
			client.setUserContext(user);

			channel = client.loadChannelFromConfig(channelName, networkConfig);

			//service discovery function.
			//Peer p = channel.getPeers().iterator().next();
			//channel.removePeer(p);
			//channel.addPeer(p, Channel.PeerOptions.createPeerOptions().addPeerRole(Peer.PeerRole.SERVICE_DISCOVERY));
			//Collection<String> cc = channel.getDiscoveredChaincodeNames();

			channel.initialize();

			channel.registerBlockListener(blockEvent -> {
				logger.info(String.format("Receive block event (number %s) from %s", blockEvent.getBlockNumber(), blockEvent.getPeer()));
			});
			printChannelInfo(client, channel);

			//executeChaincode(client, channel);

			//logger.info("Shutdown channel.");
			//channel.shutdown(true);

		} catch (Exception e) {
			logger.error("exception", e);
		}
	}
	private static void lineBreak() {
		logger.info("=============================================================");
	}

	private static void executeChaincode(HFClient client, Channel channel) throws
			ProposalException, InvalidArgumentException, UnsupportedEncodingException, InterruptedException,
			ExecutionException, TimeoutException
	{
		lineBreak();
		ChaincodeExecuter executer = new ChaincodeExecuter(chaincodeName, chaincodeVersion);

		String newValue = String.valueOf(new Random().nextInt(1000));

		executer.executeTransaction(client, channel, true,"invoke", "b", "a", "10");
		executer.executeTransaction(client, channel, false,"query", "a");
		executer.executeTransaction(client, channel, false,"query", "b");

		lineBreak();

		executer.executeTransaction(client, channel, true,"invoke", "a", "b", "10");
		executer.executeTransaction(client, channel, false,"query", "a");
		executer.executeTransaction(client, channel, false,"query", "b");


	}
	private static void printChannelInfo(HFClient client, Channel channel) throws
			ProposalException, InvalidArgumentException, IOException
	{
		lineBreak();
		BlockchainInfo channelInfo = channel.queryBlockchainInfo();

		logger.info("Channel height: " + channelInfo.getHeight());
		for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
			BlockInfo returnedBlock = channel.queryBlockByNumber(current);
			final long blockNumber = returnedBlock.getBlockNumber();

			logger.info(String.format("Block #%d has previous hash id: %s", blockNumber, Hex.encodeHexString(returnedBlock.getPreviousHash())));
			logger.info(String.format("Block #%d has data hash: %s", blockNumber, Hex.encodeHexString(returnedBlock.getDataHash())));
			logger.info(String.format("Block #%d has calculated block hash is %s",
					blockNumber, Hex.encodeHexString(SDKUtils.calculateBlockHash(client,blockNumber, returnedBlock.getPreviousHash(), returnedBlock.getDataHash()))));
		}

	}

	// Case 1 : Enroll Here!
	private static FabricUser getFabricUser(NetworkConfig.OrgInfo clientOrg, NetworkConfig.CAInfo caInfo) throws
			MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException, InfoException,
			EnrollmentException, IOException
	{
		HFCAClient hfcaClient = HFCAClient.createNewInstance(caInfo);
		HFCAInfo cainfo = hfcaClient.info();
		lineBreak();
		logger.info("CA name: " + cainfo.getCAName());
		logger.info("CA version: " + cainfo.getVersion());

		// Persistence is not part of SDK.

		logger.info("Going to enroll user: " + userName);
		Enrollment enrollment = hfcaClient.enroll(userName, secret);
		logger.info("Enroll user: " + userName +  " successfully.");

		FabricUser user = new FabricUser();
		user.setMspId(clientOrg.getMspId());
		user.setName(userName);
		user.setOrganization(clientOrg.getName());
		user.setEnrollment(enrollment);
		return user;
	}

	// Case 2 : Already Enrolled at Fabric-CA
	private static FabricUser getFabricUser4Local(NetworkConfig.OrgInfo clientOrg) throws
			IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException
	{
		lineBreak();

		String certificate = new String(IOUtils.toByteArray(new FileInputStream(new File("./cert.pem"))), "UTF-8");
		File privatekeyfile = findFileSk( new File("./"));

		EnrolleMentImpl enrollment = null;
		try {
			PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privatekeyfile)));
			enrollment = new EnrolleMentImpl(privateKey, certificate);
		} catch(Exception e){
			e.printStackTrace();
		}

		FabricUser user = new FabricUser();
		user.setMspId(clientOrg.getMspId());
		user.setName(userName);
		user.setOrganization(clientOrg.getName());
		user.setEnrollment(enrollment);
		return user;
	}

	static final class EnrolleMentImpl implements Enrollment, Serializable {
		private static final long serialVersionUID = -2784835212445309006L;
		private final PrivateKey privateKey;
		private final String certificate;

		public EnrolleMentImpl(PrivateKey privateKey, String certificate) {
			this.certificate = certificate;
			this.privateKey = privateKey;
		}

		@Override
		public PrivateKey getKey() {
			return privateKey;
		}

		@Override
		public String getCert() {
			return certificate;
		}
	}

	public static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException {
		return getPrivateKeyFromString(new String(data));
	}

	private static PrivateKey getPrivateKeyFromString(String data) throws IOException {
		final PEMParser pemParser = new PEMParser(new StringReader(data));

		Object pemObject = pemParser.readObject();

		PrivateKeyInfo privateKeyInfo;

		if (pemObject instanceof PrivateKeyInfo) {
			privateKeyInfo = (PrivateKeyInfo) pemObject;

		} else if (pemObject instanceof PEMKeyPair) {
			privateKeyInfo = ((PEMKeyPair) pemObject).getPrivateKeyInfo();
		} else {
			throw new IllegalArgumentException(String.format("Unknown private key format: %s", pemObject.getClass().toString()));
		}

		return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
	}

	public static File findFileSk(File directory) {
		File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

		if (null == matches) {
			throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
		}

		if (matches.length != 1) {
			throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
		}

		return matches[0];
	}
}
