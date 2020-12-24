package fleetmanagement.config;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
class Command {
    String date;
    int vehicles;
    boolean geo;
    boolean vehicleGeo;
    String[] packages;
    boolean diagnosis;
    boolean operation;
    boolean autoPackageSync;
    boolean upload;
    boolean notifications;
    boolean https;
    boolean reports;
    String[] languages;
    boolean vehicleIp;
}
