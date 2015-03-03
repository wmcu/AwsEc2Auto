/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * Modified by Sambit Sahu
 * Modified by Kyung-Hwa Kim (kk2515@columbia.edu)
 * 
 * 
 */
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// Use JSch library for ssh programmatically
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class AwsSample {

	/*
	 * Important: Be sure to fill in your AWS access credentials in the
	 * AwsCredentials.properties file before you try to run this sample.
	 * http://aws.amazon.com/security-credentials
	 */

	static AmazonEC2 ec2;

	public static void main(String[] args) throws Exception {

		AWSCredentials credentials = null;
		credentials = new PropertiesCredentials(
				AwsSample.class.getResourceAsStream("aws.credentials"));

		/*********************************************
		 * 
		 * #1 Create Amazon Client object
		 * 
		 *********************************************/
		System.out.println("#1 Create Amazon Client object");
		ec2 = new AmazonEC2Client(credentials);

		try {

			/*********************************************
			 * 
			 * #2 Describe Availability Zones.
			 * 
			 *********************************************/
			System.out.println("#2 Describe Availability Zones.");
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2
					.describeAvailabilityZones();
			System.out.println("You have access to "
					+ availabilityZonesResult.getAvailabilityZones().size()
					+ " Availability Zones.");

			/*********************************************
			 * 
			 * #3 Describe Available Images
			 * 
			 *********************************************/

//			 System.out.println("#3 Describe Available Images");
//			 DescribeImagesResult dir = ec2.describeImages();
//			 System.out.println("#3.5 Describe Current Instances");
//			 List<Image> images = dir.getImages();
//			 System.out.println("You have " + images.size() + " Amazon images");

			/*********************************************
			 * 
			 * #4 Describe Key Pair
			 * 
			 *********************************************/
			System.out.println("#4 Describe Key Pair");
			DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
			System.out.println(dkr.toString());

			/*********************************************
			 * 
			 * #5 Describe Current Instances
			 * 
			 *********************************************/
			System.out.println("#5 Describe Current Instances");
			DescribeInstancesResult describeInstancesRequest = ec2
					.describeInstances();
			List<Reservation> reservations = describeInstancesRequest
					.getReservations();
			Set<Instance> instances = new HashSet<Instance>();
			// add all instances to a Set.
			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
			}

			System.out.println("You have " + instances.size()
					+ " Amazon EC2 instance(s).");
			for (Instance ins : instances) {

				// instance id
				String instanceId = ins.getInstanceId();

				// instance state
				InstanceState is = ins.getState();
				System.out.println(instanceId + " " + is.getName());
			}

			/*********************************************
			 * 
			 * #6 Create a Security Group
			 * 
			 *********************************************/
			System.out.println("#6 Create a Security Group");
			CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
			String securityGroup = "HW2SecurityGroup";
			// new security group
			csgr.withGroupName(securityGroup).withDescription(
					"My security group for HW2");
			CreateSecurityGroupResult createSecurityGroupResult = ec2
					.createSecurityGroup(csgr);
			// create ip permissions
			IpPermission ipp1 = new IpPermission();
			ipp1.withIpRanges("0.0.0.0/0")
						.withIpProtocol("tcp")
						.withFromPort(22) // enable SSH
						.withToPort(22);
			IpPermission ipp2 = new IpPermission();
			ipp2.withIpRanges("0.0.0.0/0")
						.withIpProtocol("tcp")
						.withFromPort(80) // enable HTTP
						.withToPort(80);
			IpPermission ipp3 = new IpPermission();
			ipp3.withIpRanges("0.0.0.0/0")
						.withIpProtocol("tcp") // enable all TCP
						.withFromPort(0)
						.withToPort(65535);
			AuthorizeSecurityGroupIngressRequest asgir = new AuthorizeSecurityGroupIngressRequest();
			asgir.withGroupName(securityGroup)
					.withIpPermissions(ipp1)
					.withIpPermissions(ipp2)
			        .withIpPermissions(ipp3);
			ec2.authorizeSecurityGroupIngress(asgir);
			System.out.println("Security Group " + securityGroup + " created");

			/*********************************************
			 * 
			 * #7 Create a Key Pair
			 * 
			 *********************************************/
			System.out.println("#7 Create a Key Pair");
			CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
			String keyName = "HW2KeyPair";
			createKeyPairRequest.withKeyName(keyName);
			CreateKeyPairResult createKeyPairResult = ec2
					.createKeyPair(createKeyPairRequest);
			KeyPair keyPair = new KeyPair();
			keyPair = createKeyPairResult.getKeyPair();
			String privateKey = keyPair.getKeyMaterial();

			// print private key to file
			PrintWriter fout = new PrintWriter(keyName + ".pem");
			fout.println(privateKey);
			fout.close();
			System.out.println("Key Pair is saved to " + keyName + ".pem");

			/*********************************************
			 * 
			 * #8 Create an Instance
			 * 
			 *********************************************/
			System.out.println("#8 Create an Instance");
			String imageId = "ami-76f0061f"; // Basic 32-bit Amazon Linux AMI
			int minInstanceCount = 1; // create 1 instance
			int maxInstanceCount = 1;
			RunInstancesRequest rir = new RunInstancesRequest(imageId,
					minInstanceCount, maxInstanceCount);
			rir.withInstanceType("t1.micro")
			   .withKeyName(keyName).withSecurityGroups(securityGroup);
			RunInstancesResult result = ec2.runInstances(rir);

			// get instanceId from the result
			Instance resultInstance = result.getReservation().getInstances()
					.get(0);
			String instanceId = resultInstance.getInstanceId();
			System.out.println("New instance has been created: " + instanceId);

			// check status of new instance
			DescribeInstanceStatusRequest disRequest = new DescribeInstanceStatusRequest();
			disRequest.withInstanceIds(instanceId);
			DescribeInstanceStatusResult disResult = ec2
					.describeInstanceStatus(disRequest);
			List<InstanceStatus> state = disResult.getInstanceStatuses();

			// polling until the instance is running
			while (state.isEmpty()) {
				TimeUnit.SECONDS.sleep(1);
				disResult = ec2.describeInstanceStatus(disRequest);
				state = disResult.getInstanceStatuses();
			}
			String status = state.get(0).getInstanceState().getName();

			// get the IP of new instance
			DescribeInstancesRequest diRequest = new DescribeInstancesRequest();
			diRequest.withInstanceIds(instanceId);
			DescribeInstancesResult diResult = ec2.describeInstances(diRequest);
			resultInstance = diResult.getReservations().get(0).getInstances()
					.get(0);
			String instanceIP = resultInstance.getPublicIpAddress();

			System.out.println("Instance " + instanceId + " is " + status
					+ ", IP address is " + instanceIP);

			/*********************************************
			 * 
			 * #9 Create a 'tag' for the new instance.
			 * 
			 *********************************************/
			System.out.println("#9 Create a 'tag' for the new instance.");
			List<String> resources = new LinkedList<String>();
			List<Tag> tags = new LinkedList<Tag>();
			Tag nameTag = new Tag("Name", "MyHW2Instance");

			resources.add(instanceId);
			tags.add(nameTag);

			CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
			ec2.createTags(ctr);

			/*********************************************
			 * 
			 * #10 ssh into the new instance
			 * 
			 *********************************************/
			
			System.out.println("#10 ssh into the new instance");
			try {
				JSch jsch = new JSch();

				String user = "ec2-user";
				String host = instanceIP;
				int port = 22;
				String key = keyName + ".pem";

				jsch.addIdentity(key);
				// System.out.println("identity added ");

				Session session = jsch.getSession(user, host, port);
				// System.out.println("session created.");
				session.setConfig("StrictHostKeyChecking", "no");
				boolean go = false;
				int tryNum = 0;
				do {
					try {
						session.connect(60000);
						go = true;
					} catch (JSchException e) {
						tryNum++;
					}
				} while (!go && tryNum < 100);
				if (!go) {
					throw new Exception("session cannot connect..");
				}
				System.out.println("session connected.....");

				Channel channel = session.openChannel("shell");
				OutputStream inputstream_of_channel = channel.getOutputStream();
				PrintStream commander = new PrintStream(inputstream_of_channel,
						true);
				channel.setOutputStream(System.out, true);

				channel.connect();
				System.out.println("shell channel connected....");

				commander.print("pwd\n");
				commander.print("whoami\n");
				commander.print("exit\n");
				commander.close();

				do {
					TimeUnit.SECONDS.sleep(1);
				} while (!channel.isEOF());
				channel.disconnect();
				session.disconnect();

			} catch (Exception e) {
				System.err.println(e);
			}

			/*********************************************
			 * 
			 * #11 Terminate the new instance
			 * 
			 *********************************************/
			System.out.println("#11 Terminate the Instance");
			List<String> instanceIds = new LinkedList<String>();
			instanceIds.add(instanceId);
			TerminateInstancesRequest tir = new TerminateInstancesRequest(
					instanceIds);
			ec2.terminateInstances(tir);

			/*********************************************
			 * 
			 * #12 shutdown client object
			 * 
			 *********************************************/
			ec2.shutdown();

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		}

	}
}