Requisition requisition = new Requisition()
requisition.setForeignSource(instance)

// Loop through JSON Results
result.results.each { f ->
    logger.info("Adding node: {}", f.name )

    // Create a new requisition node
    RequisitionNode requisitionNode = new RequisitionNode()

    // IP interfaces
    List<RequisitionInterface> interfaceList = new ArrayList<RequisitionInterface>()
    RequisitionInterface requisitionInterface = new RequisitionInterface()

    // List of services
    List<RequisitionMonitoredService> monitoredServiceList = new ArrayList<RequisitionMonitoredService>()

    // Asset variables
    List<RequisitionAsset> assetList = new ArrayList<RequisitionAsset>()
    RequisitionAsset assetLatitude = new RequisitionAsset()
    RequisitionAsset assetLongitude = new RequisitionAsset()
    RequisitionAsset assetpollerCategory = new RequisitionAsset()

    // Query for Site JSON Document
    site_result =  GetJsonResult(f.site.url, token)

    // Set node label and foreign ID and Minion Location for the node
    requisitionNode.setNodeLabel(f.name)
    systemSeed = config.getString("system")
    requisitionNode.setForeignId(systemSeed + f.id.toString())


    // Assign Default Location
    // Datasource requisition.properties
    config.getStringArray("location").each { s ->
        logger.info("Setting Default Location: {}", s)
        requisitionNode.setLocation(s)
    }

    // Set Minion Location for the node from a tag
    site_result.tags.each { t ->
        def locationTag = t
        switch( locationTag ) {
             case ~/^ONSL_(.*)$/:
                logger.info("Override Location")
                requisitionNode.setLocation("${Matcher.lastMatcher[0][1]}");
        }
    }

    f.tags.each { t ->
        def deviceTag = t
        logger.info("-- Location Tag Check: {}", deviceTag.name )
        switch( deviceTag.name ) {
            case ~/^ONSL_(.*)$/:
                logger.info("Override Location")
                requisitionNode.setLocation("${Matcher.lastMatcher[0][1]}");
        }
    }

    // Create IP interface and set status monitored (1 managed / 3 is not managed)
    if(f.primary_ip != null)  {
        requisitionInterface.setStatus(1);
        requisitionInterface.setIpAddr(f.primary_ip.address.split('/')[0])
        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY)
        interfaceList.add(requisitionInterface)
    }

    // Assign services to monitor to interface
    // Datasource requisition.properties
    config.getStringArray("services").each { s ->
          monitoredServiceList.add(new RequisitionMonitoredService(null, s))
          logger.info("Adding Service: {}", s )
    }

    // Assign services to from device tags
    f.tags.each { t ->
        def deviceTag = t
        logger.info("Services Check Tag: {}", deviceTag.name )
        switch( deviceTag.name ) {
            case ~/^ONSP_(.*)$/:
                logger.info("Setting Additional Service")
                monitoredServiceList.add(new RequisitionMonitoredService(null, "${Matcher.lastMatcher[0][1]}"))
        }
    }

    // Assign poller Category to from device tags
    f.tags.each { t ->
        def deviceTag = t
        logger.info("Category Tag Check: {}", deviceTag.name )
        switch( deviceTag.name ) {
            case ~/^ONSC_(.*)$/:
                logger.info("Setting Category")
                assetpollerCategory.setName("pollerCategory")
                assetpollerCategory.setValue("${Matcher.lastMatcher[0][1]}")
                assetList.add(assetpollerCategory)
        }
    }

    // Assign services for monitoring to IP interface
    requisitionInterface.getMonitoredServices().addAll(monitoredServiceList)

    // Set Asset information for Longitude and Latitude
    if (site_result.latitude != null ) {
        assetLatitude.setName("latitude")
        assetLatitude.setValue(site_result.latitude)
        assetList.add(assetLatitude)
    }
    if (site_result.longitude != null ) {
        assetLongitude.setName("longitude")
        assetLongitude.setValue(site_result.longitude)
        assetList.add(assetLongitude)
    }

    // Assign Interfaces and assets to the node
    requisitionNode.getInterfaces().addAll(interfaceList)
    requisitionNode.getAssets().addAll(assetList)

    // Put new node into requisition
    requisition.getNodes().add(requisitionNode)
}

return requisition
