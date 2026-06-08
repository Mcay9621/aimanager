package com.example.aimanager.service.cloud;

import com.example.aimanager.entity.CloudAccount;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.cvm.v20170312.CvmClient;
import com.tencentcloudapi.cvm.v20170312.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentCloudClient implements CloudClient {

    private static final Logger log = LoggerFactory.getLogger(TencentCloudClient.class);

    @Override
    public boolean supports(String provider) {
        return "tencent".equals(provider);
    }

    @Override
    public List<Map<String, Object>> describeInstances(CloudAccount account) {
        List<Map<String, Object>> instances = describeInstancesInRegion(account, account.getRegion());
        log.info("腾讯云[{}] 默认区域[{}] 找到 {} 个实例", account.getAliasName(), account.getRegion(), instances.size());
        return instances;
    }

    private List<Map<String, Object>> describeInstancesInRegion(CloudAccount account, String region) {
        if (!hasValidCredentials(account)) return Collections.emptyList();
        try {
            Credential cred = new Credential(account.getAccessKey(), account.getAccessSecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("cvm.tencentcloudapi.com");
            httpProfile.setReadTimeout(10000);
            httpProfile.setConnTimeout(5000);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            CvmClient client = new CvmClient(cred, region, clientProfile);
            DescribeInstancesRequest req = new DescribeInstancesRequest();
            req.setLimit(50L);

            DescribeInstancesResponse resp = client.DescribeInstances(req);
            List<Map<String, Object>> instances = new ArrayList<>();

            for (Instance instance : resp.getInstanceSet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", instance.getInstanceId());
                item.put("name", instance.getInstanceName());
                item.put("provider", "tencent");
                item.put("type", "bcc");
                item.put("status", mapStatus(instance.getInstanceState()));
                item.put("cpu", String.valueOf(instance.getCPU()));
                item.put("memory", String.valueOf(instance.getMemory()));
                item.put("publicIp", instance.getPublicIpAddresses() != null && instance.getPublicIpAddresses().length > 0
                        ? instance.getPublicIpAddresses()[0] : "");
                item.put("privateIp", instance.getPrivateIpAddresses() != null && instance.getPrivateIpAddresses().length > 0
                        ? instance.getPrivateIpAddresses()[0] : "");
                item.put("region", region);
                item.put("accountAlias", account.getAliasName());
                item.put("osName", instance.getOsName());
                item.put("createTime", instance.getCreatedTime());
                item.put("expireTime", instance.getExpiredTime());
                instances.add(item);
            }
            return instances;

        } catch (TencentCloudSDKException e) {
            log.warn("腾讯云[{}] DescribeInstances 失败: {}", region, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void startInstance(CloudAccount account, String instanceId) {
        try {
            Credential cred = new Credential(account.getAccessKey(), account.getAccessSecret());
            CvmClient client = new CvmClient(cred, account.getRegion());
            StartInstancesRequest req = new StartInstancesRequest();
            req.setInstanceIds(new String[]{instanceId});
            client.StartInstances(req);
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("启动实例失败: " + e.getMessage());
        }
    }

    @Override
    public void stopInstance(CloudAccount account, String instanceId) {
        try {
            Credential cred = new Credential(account.getAccessKey(), account.getAccessSecret());
            CvmClient client = new CvmClient(cred, account.getRegion());
            StopInstancesRequest req = new StopInstancesRequest();
            req.setInstanceIds(new String[]{instanceId});
            client.StopInstances(req);
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("停止实例失败: " + e.getMessage());
        }
    }

    @Override
    public void rebootInstance(CloudAccount account, String instanceId) {
        try {
            Credential cred = new Credential(account.getAccessKey(), account.getAccessSecret());
            CvmClient client = new CvmClient(cred, account.getRegion());
            RebootInstancesRequest req = new RebootInstancesRequest();
            req.setInstanceIds(new String[]{instanceId});
            client.RebootInstances(req);
        } catch (TencentCloudSDKException e) {
            throw new RuntimeException("重启实例失败: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection(CloudAccount account) {
        if (!hasValidCredentials(account)) return false;
        try {
            Credential cred = new Credential(account.getAccessKey(), account.getAccessSecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("cvm.tencentcloudapi.com");
            httpProfile.setReadTimeout(5000);
            httpProfile.setConnTimeout(5000);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            CvmClient client = new CvmClient(cred, account.getRegion(), clientProfile);
            DescribeRegionsRequest req = new DescribeRegionsRequest();
            client.DescribeRegions(req);
            return true;
        } catch (TencentCloudSDKException e) {
            return false;
        }
    }

    private boolean hasValidCredentials(CloudAccount account) {
        return account.getAccessKey() != null && !account.getAccessKey().isEmpty()
                && account.getAccessSecret() != null && !account.getAccessSecret().isEmpty();
    }

    private String mapStatus(String instanceState) {
        if (instanceState == null) return "error";
        return switch (instanceState) {
            case "RUNNING" -> "running";
            case "STOPPED" -> "stopped";
            case "STARTING" -> "running";
            case "STOPPING" -> "stopped";
            default -> "error";
        };
    }
}
