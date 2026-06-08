package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimanager.entity.*;
import com.example.aimanager.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class CloudResourceService {

    private final Map<String, TableHandler> typeHandlers = new LinkedHashMap<>();
    private final Map<String, FieldExtractor> fieldExtractors = new LinkedHashMap<>();
    private final Map<String, FieldExtractor> detailFieldExtractors = new LinkedHashMap<>();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CloudResourceService(
            CloudResourceCvmMapper cvmMapper,
            CloudResourceBmsMapper bmsMapper,
            CloudResourceCbsMapper cbsMapper,
            CloudResourceCfsMapper cfsMapper,
            CloudResourceCosMapper cosMapper,
            CloudResourceSnapshotMapper snapshotMapper,
            CloudResourceMysqlMapper mysqlMapper,
            CloudResourceOracleMapper oracleMapper,
            CloudResourceRedisMapper redisMapper,
            CloudResourceVpcMapper vpcMapper,
            CloudResourceVpnMapper vpnMapper,
            CloudResourceNatGatewayMapper natGatewayMapper,
            CloudResourceEipMapper eipMapper,
            CloudResourceClbMapper clbMapper,
            CloudResourceCdnMapper cdnMapper,
            CloudResourceSubnetMapper subnetMapper,
            CloudResourceSecurityGroupMapper securityGroupMapper,
            CloudResourceRouteTableMapper routeTableMapper,
            CloudResourceDirectConnectMapper directConnectMapper,
            CloudResourceLogicalDirectConnectMapper logicalDirectConnectMapper,
            CloudResourceDirectConnectTunnelMapper directConnectTunnelMapper,
            CloudResourcePeeringConnectionMapper peeringConnectionMapper,
            CloudResourceEniMapper eniMapper,
            CloudResourceDirectConnectGatewayMapper directConnectGatewayMapper,
            CloudResourceBastionMapper bastionMapper,
            CloudResourceDnsMapper dnsMapper,
            CloudResourceSslMapper sslMapper,
            CloudResourceWafMapper wafMapper,
            CloudResourceDdosMapper ddosMapper,
            CloudResourceTkeMapper tkeMapper,
            CloudResourceImageRegistryMapper imageRegistryMapper,
            CloudResourceAutoScalingMapper autoScalingMapper,
            CloudResourceFunctionMapper functionMapper,
            CloudResourceMqMapper mqMapper,
            CloudResourceLogServiceMapper logServiceMapper,
            CloudResourceCloudMonitorMapper cloudMonitorMapper,
            CloudResourceGlobalAccelerationMapper globalAccelerationMapper,
            CloudResourceKmsMapper kmsMapper,
            CloudResourceDtsMapper dtsMapper) {

        register("cvm", cvmMapper, CloudResourceCvm.class);
        register("bms", bmsMapper, CloudResourceBms.class);
        register("cbs", cbsMapper, CloudResourceCbs.class);
        register("cfs", cfsMapper, CloudResourceCfs.class);
        register("cos", cosMapper, CloudResourceCos.class);
        register("snapshot", snapshotMapper, CloudResourceSnapshot.class);
        register("mysql", mysqlMapper, CloudResourceMysql.class);
        register("oracle", oracleMapper, CloudResourceOracle.class);
        register("redis", redisMapper, CloudResourceRedis.class);
        register("vpc", vpcMapper, CloudResourceVpc.class);
        register("vpn", vpnMapper, CloudResourceVpn.class);
        register("nat", natGatewayMapper, CloudResourceNatGateway.class);
        register("eip", eipMapper, CloudResourceEip.class);
        register("clb", clbMapper, CloudResourceClb.class);
        register("cdn", cdnMapper, CloudResourceCdn.class);
        register("subnet", subnetMapper, CloudResourceSubnet.class);
        register("sg", securityGroupMapper, CloudResourceSecurityGroup.class);
        register("route_table", routeTableMapper, CloudResourceRouteTable.class);
        register("dc", directConnectMapper, CloudResourceDirectConnect.class);
        register("ldc", logicalDirectConnectMapper, CloudResourceLogicalDirectConnect.class);
        register("dc_tunnel", directConnectTunnelMapper, CloudResourceDirectConnectTunnel.class);
        register("peering", peeringConnectionMapper, CloudResourcePeeringConnection.class);
        register("eni", eniMapper, CloudResourceEni.class);
        register("dc_gateway", directConnectGatewayMapper, CloudResourceDirectConnectGateway.class);
        register("bastion", bastionMapper, CloudResourceBastion.class);
        register("dns", dnsMapper, CloudResourceDns.class);
        register("ssl", sslMapper, CloudResourceSsl.class);
        register("waf", wafMapper, CloudResourceWaf.class);
        register("ddos", ddosMapper, CloudResourceDdos.class);
        register("tke", tkeMapper, CloudResourceTke.class);
        register("image_registry", imageRegistryMapper, CloudResourceImageRegistry.class);
        register("as", autoScalingMapper, CloudResourceAutoScaling.class);
        register("function", functionMapper, CloudResourceFunction.class);
        register("mq", mqMapper, CloudResourceMq.class);
        register("log_service", logServiceMapper, CloudResourceLogService.class);
        register("cloud_monitor", cloudMonitorMapper, CloudResourceCloudMonitor.class);
        register("global_acceleration", globalAccelerationMapper, CloudResourceGlobalAcceleration.class);
        register("kms", kmsMapper, CloudResourceKms.class);
        register("dts", dtsMapper, CloudResourceDts.class);
        initFieldExtractors();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void register(String type, BaseMapper mapper, Class cls) {
        Function<Long, List<?>> queryFn = accountId -> selectByAccount(mapper, accountId);
        Function<Long, Object> getByIdFn = id -> mapper.selectById(id);
        typeHandlers.put(type, new TableHandler(type, queryFn, getByIdFn));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<?> selectByAccount(BaseMapper mapper, Long accountId) {
        QueryWrapper<Object> qw = new QueryWrapper<>();
        qw.eq("account_id", accountId);
        return mapper.selectList(qw);
    }

    public List<Map<String, Object>> getResourcesByAccounts(List<CloudAccount> accounts, String providerFilter) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CloudAccount account : accounts) {
            if (account.getStatus() != 1) continue;
            if (providerFilter != null && !providerFilter.isEmpty()
                    && !providerFilter.equals(account.getProvider())) continue;

            for (TableHandler handler : typeHandlers.values()) {
                List<?> entities = handler.queryFn.apply(account.getId());
                for (Object entity : entities) {
                    Map<String, Object> map = toMap((BaseResource) entity, handler.type);
                    if (map != null) {
                        map.put("accountAlias", account.getAliasName());
                        result.add(map);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResourceDetail(String compositeId) {
        int dash = compositeId.indexOf('-');
        if (dash < 0) return null;
        String type = compositeId.substring(0, dash);
        try {
            Long id = Long.parseLong(compositeId.substring(dash + 1));
            TableHandler handler = typeHandlers.get(type);
            if (handler == null) return null;
            Object entity = handler.getByIdFn.apply(id);
            if (entity == null) return null;
            return toDetailMap((BaseResource) entity, type);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== Map conversion ==========

    private void putCommon(Map<String, Object> map, BaseResource e, String type) {
        map.put("id", type + "-" + e.getId());
        map.put("resourceType", type);
        map.put("provider", e.getProvider());
        map.put("status", e.getStatus());
        map.put("region", e.getRegion());
        if (e.getCreateTime() != null) map.put("createTime", e.getCreateTime().format(DTF));
    }

    private String safeStr(Object v) { return v != null ? v.toString() : ""; }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(BaseResource entity, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        putCommon(map, entity, type);
        FieldExtractor extractor = fieldExtractors.get(type);
        if (extractor != null) {
            extractor.extract(map, entity);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toDetailMap(BaseResource entity, String type) {
        Map<String, Object> map = toMap(entity, type);
        if (map == null) return null;

        FieldExtractor extractor = detailFieldExtractors.get(type);
        if (extractor != null) {
            extractor.extract(map, entity);
        }
        return map;
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DTF) : "";
    }

    private record TableHandler(String type,
                                Function<Long, List<?>> queryFn,
                                Function<Long, Object> getByIdFn) {}

    @FunctionalInterface
    private interface FieldExtractor {
        void extract(Map<String, Object> map, BaseResource entity);
    }

    private void registerFieldExtractor(String type, BiConsumer<Map<String, Object>, BaseResource> listFields,
                                         BiConsumer<Map<String, Object>, BaseResource> detailFields) {
        fieldExtractors.put(type, (map, entity) -> listFields.accept(map, entity));
        if (detailFields != null) {
            detailFieldExtractors.put(type, (map, entity) -> detailFields.accept(map, entity));
        }
    }

    private void initFieldExtractors() {
        registerFieldExtractor("cvm",
            (m, e) -> { var r = (CloudResourceCvm) e; m.put("name", r.getName()); m.put("cpu", r.getCpu()); m.put("memory", r.getMemory()); m.put("publicIp", safeStr(r.getPublicIp())); m.put("privateIp", safeStr(r.getPrivateIp())); m.put("osName", safeStr(r.getOsName())); m.put("instanceId", safeStr(r.getInstanceId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceCvm) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("vpcId", safeStr(r.getVpcId())); m.put("subnetId", safeStr(r.getSubnetId())); m.put("imageId", safeStr(r.getImageId())); });
        registerFieldExtractor("bms",
            (m, e) -> { var r = (CloudResourceBms) e; m.put("name", r.getName()); m.put("cpu", r.getCpu()); m.put("memory", r.getMemory()); m.put("publicIp", safeStr(r.getPublicIp())); m.put("privateIp", safeStr(r.getPrivateIp())); m.put("osName", safeStr(r.getOsName())); m.put("instanceId", safeStr(r.getInstanceId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceBms) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("vpcId", safeStr(r.getVpcId())); m.put("subnetId", safeStr(r.getSubnetId())); m.put("networkCardCount", r.getNetworkCardCount()); m.put("raidConfig", safeStr(r.getRaidConfig())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("cbs",
            (m, e) -> { var r = (CloudResourceCbs) e; m.put("name", r.getName()); m.put("sizeGb", r.getSizeGb()); m.put("diskType", safeStr(r.getDiskType())); m.put("instanceId", safeStr(r.getInstanceId())); m.put("zone", safeStr(r.getZone())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceCbs) e; m.put("diskId", safeStr(r.getDiskId())); m.put("diskType", safeStr(r.getDiskType())); m.put("sizeGb", r.getSizeGb()); m.put("zone", safeStr(r.getZone())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("cfs",
            (m, e) -> { var r = (CloudResourceCfs) e; m.put("name", r.getName()); m.put("sizeGb", r.getSizeGb()); m.put("usedGb", r.getUsedGb()); m.put("protocol", safeStr(r.getProtocol())); m.put("storageType", safeStr(r.getStorageType())); m.put("mountPoint", safeStr(r.getMountPoint())); },
            (m, e) -> { var r = (CloudResourceCfs) e; m.put("fileSystemId", safeStr(r.getFileSystemId())); m.put("protocol", safeStr(r.getProtocol())); m.put("storageType", safeStr(r.getStorageType())); m.put("sizeGb", r.getSizeGb()); m.put("usedGb", r.getUsedGb()); m.put("mountPoint", safeStr(r.getMountPoint())); m.put("vpcId", safeStr(r.getVpcId())); });
        registerFieldExtractor("cos",
            (m, e) -> { var r = (CloudResourceCos) e; m.put("name", r.getName()); m.put("bucketName", safeStr(r.getBucketName())); m.put("storageClass", safeStr(r.getStorageClass())); m.put("objectCount", r.getObjectCount()); m.put("sizeGb", r.getSizeGb()); },
            (m, e) -> { var r = (CloudResourceCos) e; m.put("bucketName", safeStr(r.getBucketName())); m.put("storageClass", safeStr(r.getStorageClass())); m.put("objectCount", r.getObjectCount()); m.put("sizeGb", r.getSizeGb()); });
        registerFieldExtractor("snapshot",
            (m, e) -> { var r = (CloudResourceSnapshot) e; m.put("name", r.getName()); m.put("diskId", safeStr(r.getDiskId())); m.put("diskSizeGb", r.getDiskSizeGb()); },
            (m, e) -> { var r = (CloudResourceSnapshot) e; m.put("snapshotId", safeStr(r.getSnapshotId())); m.put("diskId", safeStr(r.getDiskId())); m.put("diskSizeGb", r.getDiskSizeGb()); });
        registerFieldExtractor("mysql",
            (m, e) -> { var r = (CloudResourceMysql) e; m.put("name", r.getName()); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("instanceId", safeStr(r.getInstanceId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceMysql) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("vpcId", safeStr(r.getVpcId())); m.put("subnetId", safeStr(r.getSubnetId())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("oracle",
            (m, e) -> { var r = (CloudResourceOracle) e; m.put("name", r.getName()); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("instanceId", safeStr(r.getInstanceId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceOracle) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("vpcId", safeStr(r.getVpcId())); m.put("subnetId", safeStr(r.getSubnetId())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("redis",
            (m, e) -> { var r = (CloudResourceRedis) e; m.put("name", r.getName()); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("networkType", safeStr(r.getNetworkType())); m.put("instanceId", safeStr(r.getInstanceId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceRedis) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("engineVersion", safeStr(r.getEngineVersion())); m.put("spec", safeStr(r.getSpec())); m.put("storageGb", r.getStorageGb()); m.put("networkType", safeStr(r.getNetworkType())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("vpc",
            (m, e) -> { var r = (CloudResourceVpc) e; m.put("name", r.getName()); m.put("cidr", safeStr(r.getCidr())); },
            null);
        registerFieldExtractor("vpn",
            (m, e) -> { var r = (CloudResourceVpn) e; m.put("name", r.getName()); m.put("publicIp", safeStr(r.getPublicIp())); m.put("vpcId", safeStr(r.getVpcId())); },
            null);
        registerFieldExtractor("nat",
            (m, e) -> { var r = (CloudResourceNatGateway) e; m.put("name", r.getName()); m.put("vpcId", safeStr(r.getVpcId())); m.put("publicIps", safeStr(r.getPublicIps())); },
            null);
        registerFieldExtractor("eip",
            (m, e) -> { var r = (CloudResourceEip) e; m.put("name", r.getName()); m.put("publicIp", safeStr(r.getPublicIp())); m.put("bandwidth", r.getBandwidth()); m.put("instanceId", safeStr(r.getInstanceId())); },
            null);
        registerFieldExtractor("clb",
            (m, e) -> { var r = (CloudResourceClb) e; m.put("name", r.getName()); m.put("address", safeStr(r.getAddress())); m.put("type", safeStr(r.getType())); },
            null);
        registerFieldExtractor("cdn",
            (m, e) -> { var r = (CloudResourceCdn) e; m.put("name", r.getName()); m.put("domain", safeStr(r.getDomain())); m.put("cname", safeStr(r.getCname())); },
            null);
        registerFieldExtractor("subnet",
            (m, e) -> { var r = (CloudResourceSubnet) e; m.put("name", r.getName()); m.put("cidr", safeStr(r.getCidr())); m.put("zone", safeStr(r.getZone())); m.put("availableIpCount", r.getAvailableIpCount()); m.put("vpcId", safeStr(r.getVpcId())); },
            null);
        registerFieldExtractor("sg",
            (m, e) -> { var r = (CloudResourceSecurityGroup) e; m.put("name", r.getName()); m.put("vpcId", safeStr(r.getVpcId())); m.put("description", safeStr(r.getDescription())); },
            null);
        registerFieldExtractor("route_table",
            (m, e) -> { var r = (CloudResourceRouteTable) e; m.put("name", r.getName()); m.put("vpcId", safeStr(r.getVpcId())); },
            null);
        registerFieldExtractor("dc",
            (m, e) -> { var r = (CloudResourceDirectConnect) e; m.put("name", r.getName()); m.put("bandwidth", r.getBandwidth()); m.put("circuitCode", safeStr(r.getCircuitCode())); m.put("providerName", safeStr(r.getProviderName())); },
            null);
        registerFieldExtractor("ldc",
            (m, e) -> { var r = (CloudResourceLogicalDirectConnect) e; m.put("name", r.getName()); m.put("bandwidth", r.getBandwidth()); m.put("dcId", safeStr(r.getDcId())); m.put("vlan", r.getVlan()); },
            null);
        registerFieldExtractor("dc_tunnel",
            (m, e) -> { var r = (CloudResourceDirectConnectTunnel) e; m.put("name", r.getName()); m.put("bandwidth", r.getBandwidth()); m.put("dcId", safeStr(r.getDcId())); m.put("vlan", r.getVlan()); },
            null);
        registerFieldExtractor("peering",
            (m, e) -> { var r = (CloudResourcePeeringConnection) e; m.put("name", r.getName()); m.put("peerVpcId", safeStr(r.getPeerVpcId())); m.put("peerRegion", safeStr(r.getPeerRegion())); m.put("bandwidth", r.getBandwidth()); m.put("vpcId", safeStr(r.getVpcId())); },
            null);
        registerFieldExtractor("eni",
            (m, e) -> { var r = (CloudResourceEni) e; m.put("name", r.getName()); m.put("privateIps", safeStr(r.getPrivateIps())); m.put("mac", safeStr(r.getMac())); m.put("vpcId", safeStr(r.getVpcId())); m.put("subnetId", safeStr(r.getSubnetId())); m.put("instanceId", safeStr(r.getInstanceId())); },
            null);
        registerFieldExtractor("dc_gateway",
            (m, e) -> { var r = (CloudResourceDirectConnectGateway) e; m.put("name", r.getName()); m.put("vpcId", safeStr(r.getVpcId())); },
            null);
        registerFieldExtractor("bastion",
            (m, e) -> { var r = (CloudResourceBastion) e; m.put("name", r.getName()); m.put("spec", safeStr(r.getSpec())); m.put("licenseCount", r.getLicenseCount()); m.put("publicIp", safeStr(r.getPublicIp())); m.put("privateIp", safeStr(r.getPrivateIp())); m.put("expireTime", fmt(r.getExpireTime())); },
            null);
        registerFieldExtractor("dns",
            (m, e) -> { var r = (CloudResourceDns) e; m.put("name", r.getDomain()); m.put("domain", safeStr(r.getDomain())); m.put("recordCount", r.getRecordCount()); m.put("dnsType", safeStr(r.getDnsType())); m.put("zoneId", safeStr(r.getZoneId())); },
            (m, e) -> { var r = (CloudResourceDns) e; m.put("zoneId", safeStr(r.getZoneId())); m.put("domain", safeStr(r.getDomain())); m.put("recordCount", r.getRecordCount()); m.put("dnsType", safeStr(r.getDnsType())); });
        registerFieldExtractor("ssl",
            (m, e) -> { var r = (CloudResourceSsl) e; m.put("name", r.getName()); m.put("domain", safeStr(r.getDomain())); m.put("issuer", safeStr(r.getIssuer())); m.put("algorithm", safeStr(r.getAlgorithm())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceSsl) e; m.put("certId", safeStr(r.getCertId())); m.put("domain", safeStr(r.getDomain())); m.put("issuer", safeStr(r.getIssuer())); m.put("algorithm", safeStr(r.getAlgorithm())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("waf",
            (m, e) -> { var r = (CloudResourceWaf) e; m.put("name", r.getName()); m.put("instanceId", safeStr(r.getInstanceId())); m.put("mode", safeStr(r.getMode())); m.put("domainCount", r.getDomainCount()); m.put("ruleCount", r.getRuleCount()); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceWaf) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("mode", safeStr(r.getMode())); m.put("domainCount", r.getDomainCount()); m.put("ruleCount", r.getRuleCount()); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("ddos",
            (m, e) -> { var r = (CloudResourceDdos) e; m.put("name", r.getName()); m.put("instanceId", safeStr(r.getInstanceId())); m.put("bandwidth", r.getBandwidth()); m.put("protectionCount", r.getProtectionCount()); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceDdos) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("bandwidth", r.getBandwidth()); m.put("protectionCount", r.getProtectionCount()); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("tke",
            (m, e) -> { var r = (CloudResourceTke) e; m.put("name", r.getName()); m.put("clusterId", safeStr(r.getClusterId())); m.put("version", safeStr(r.getVersion())); m.put("nodeCount", r.getNodeCount()); m.put("cpuTotal", r.getCpuTotal()); m.put("memoryTotal", r.getMemoryTotal()); m.put("vpcId", safeStr(r.getVpcId())); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceTke) e; m.put("clusterId", safeStr(r.getClusterId())); m.put("version", safeStr(r.getVersion())); m.put("nodeCount", r.getNodeCount()); m.put("cpuTotal", r.getCpuTotal()); m.put("memoryTotal", r.getMemoryTotal()); m.put("vpcId", safeStr(r.getVpcId())); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("image_registry",
            (m, e) -> { var r = (CloudResourceImageRegistry) e; m.put("name", r.getName()); m.put("registryId", safeStr(r.getRegistryId())); m.put("registryType", safeStr(r.getRegistryType())); m.put("repoCount", r.getRepoCount()); m.put("storageGb", r.getStorageGb()); },
            (m, e) -> { var r = (CloudResourceImageRegistry) e; m.put("registryId", safeStr(r.getRegistryId())); m.put("registryType", safeStr(r.getRegistryType())); m.put("repoCount", r.getRepoCount()); m.put("storageGb", r.getStorageGb()); });
        registerFieldExtractor("as",
            (m, e) -> { var r = (CloudResourceAutoScaling) e; m.put("name", r.getName()); m.put("scalingGroupId", safeStr(r.getScalingGroupId())); m.put("minSize", r.getMinSize()); m.put("maxSize", r.getMaxSize()); m.put("desiredSize", r.getDesiredSize()); m.put("vpcId", safeStr(r.getVpcId())); },
            (m, e) -> { var r = (CloudResourceAutoScaling) e; m.put("scalingGroupId", safeStr(r.getScalingGroupId())); m.put("minSize", r.getMinSize()); m.put("maxSize", r.getMaxSize()); m.put("desiredSize", r.getDesiredSize()); m.put("vpcId", safeStr(r.getVpcId())); });
        registerFieldExtractor("function",
            (m, e) -> { var r = (CloudResourceFunction) e; m.put("name", r.getName()); m.put("functionId", safeStr(r.getFunctionId())); m.put("runtime", safeStr(r.getRuntime())); m.put("memoryMb", r.getMemoryMb()); m.put("timeoutSec", r.getTimeoutSec()); m.put("triggerCount", r.getTriggerCount()); m.put("vpcId", safeStr(r.getVpcId())); m.put("codeSize", r.getCodeSize()); },
            (m, e) -> { var r = (CloudResourceFunction) e; m.put("functionId", safeStr(r.getFunctionId())); m.put("runtime", safeStr(r.getRuntime())); m.put("memoryMb", r.getMemoryMb()); m.put("timeoutSec", r.getTimeoutSec()); m.put("triggerCount", r.getTriggerCount()); m.put("vpcId", safeStr(r.getVpcId())); m.put("codeSize", r.getCodeSize()); });
        registerFieldExtractor("mq",
            (m, e) -> { var r = (CloudResourceMq) e; m.put("name", r.getName()); m.put("instanceId", safeStr(r.getInstanceId())); m.put("topicCount", r.getTopicCount()); m.put("queueType", safeStr(r.getQueueType())); m.put("maxStorageGb", r.getMaxStorageGb()); m.put("vpcId", safeStr(r.getVpcId())); },
            (m, e) -> { var r = (CloudResourceMq) e; m.put("instanceId", safeStr(r.getInstanceId())); m.put("topicCount", r.getTopicCount()); m.put("queueType", safeStr(r.getQueueType())); m.put("maxStorageGb", r.getMaxStorageGb()); m.put("vpcId", safeStr(r.getVpcId())); });
        registerFieldExtractor("log_service",
            (m, e) -> { var r = (CloudResourceLogService) e; m.put("name", r.getLogsetName()); m.put("logsetName", safeStr(r.getLogsetName())); m.put("logsetId", safeStr(r.getLogsetId())); m.put("topicCount", r.getTopicCount()); m.put("storageGb", r.getStorageGb()); m.put("retentionDays", r.getRetentionDays()); },
            (m, e) -> { var r = (CloudResourceLogService) e; m.put("logsetId", safeStr(r.getLogsetId())); m.put("logsetName", safeStr(r.getLogsetName())); m.put("topicCount", r.getTopicCount()); m.put("storageGb", r.getStorageGb()); m.put("retentionDays", r.getRetentionDays()); });
        registerFieldExtractor("cloud_monitor",
            (m, e) -> { var r = (CloudResourceCloudMonitor) e; m.put("name", r.getName()); m.put("alarmId", safeStr(r.getAlarmId())); m.put("metric", safeStr(r.getMetric())); m.put("threshold", safeStr(r.getThreshold())); m.put("statPeriod", r.getStatPeriod()); },
            (m, e) -> { var r = (CloudResourceCloudMonitor) e; m.put("alarmId", safeStr(r.getAlarmId())); m.put("metric", safeStr(r.getMetric())); m.put("threshold", safeStr(r.getThreshold())); m.put("statPeriod", r.getStatPeriod()); });
        registerFieldExtractor("global_acceleration",
            (m, e) -> { var r = (CloudResourceGlobalAcceleration) e; m.put("name", r.getName()); m.put("acceleratorId", safeStr(r.getAcceleratorId())); m.put("bandwidth", r.getBandwidth()); m.put("concurrentConnections", r.getConcurrentConnections()); m.put("expireTime", fmt(r.getExpireTime())); },
            (m, e) -> { var r = (CloudResourceGlobalAcceleration) e; m.put("acceleratorId", safeStr(r.getAcceleratorId())); m.put("bandwidth", r.getBandwidth()); m.put("concurrentConnections", r.getConcurrentConnections()); m.put("expireTime", fmt(r.getExpireTime())); });
        registerFieldExtractor("kms",
            (m, e) -> { var r = (CloudResourceKms) e; m.put("name", r.getName()); m.put("keyId", safeStr(r.getKeyId())); m.put("algorithm", safeStr(r.getAlgorithm())); m.put("keySpec", safeStr(r.getKeySpec())); m.put("rotationEnabled", r.getRotationEnabled()); },
            (m, e) -> { var r = (CloudResourceKms) e; m.put("keyId", safeStr(r.getKeyId())); m.put("algorithm", safeStr(r.getAlgorithm())); m.put("keySpec", safeStr(r.getKeySpec())); m.put("rotationEnabled", r.getRotationEnabled()); });
        registerFieldExtractor("dts",
            (m, e) -> { var r = (CloudResourceDts) e; m.put("name", r.getName()); m.put("taskId", safeStr(r.getTaskId())); m.put("sourceType", safeStr(r.getSourceType())); m.put("targetType", safeStr(r.getTargetType())); m.put("migrationType", safeStr(r.getMigrationType())); },
            (m, e) -> { var r = (CloudResourceDts) e; m.put("taskId", safeStr(r.getTaskId())); m.put("sourceType", safeStr(r.getSourceType())); m.put("targetType", safeStr(r.getTargetType())); m.put("migrationType", safeStr(r.getMigrationType())); });
    }
}
