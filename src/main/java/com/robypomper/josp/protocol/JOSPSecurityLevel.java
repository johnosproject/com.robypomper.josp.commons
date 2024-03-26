package com.robypomper.josp.protocol;


/**
 * Definition class for the security levels used in JOSP.<br/>
 * The security level is used to define the security used for the connection
 * between JOSP Object and JOSP Service on Local Communication.
 * <p>
 * The security level is defined by the following parameters:
 * <ul>
 *     <li>useSSL: if the connection is encrypted</li>
 *     <li>useShared: if the remote certificate can be shared</li>
 *     <li>useCertificatedId: if the remote certificate contains peer's ID</li>
 * </ul>
 * <p>
 * The security level is defined by the following values:
 * <ul>
 *     <li>SSLInstance: Use an SSL encrypted connection with a unique instance id.
 *     All certificates are validated using the default JKS.</li>
 *     <li>SSLShareInstance: Use an SSL encrypted connection with a unique instance id.
 *     Certificates can be shared on the go.</li>
 *     <li>SSLComp: Use an SSL encrypted connection with a shared instance id.
 *     All certificates are validated using the default JKS.</li>
 *     <li>SSLShareComp: Use an SSL encrypted connection with a shared instance id.
 *     Certificates can be shared on the go.</li>
 *     <li>NoSSL: Use a non-SSL connection and no certificates.</li>
 * </ul>
 * <p>
 * Here, for a more clear understanding, the possible combinations of the parameters:
 * <table>
 *     <tr>
 *         <th>Security Level</th>
 *         <th>useSSL</th>
 *         <th>useShared</th>
 *         <th>useCertificatedId</th>
 *     </tr>
 *     <tr>
 *         <td>SSLInstance</td>
 *         <td>true</td>
 *         <td>false</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td>SSLShareInstance</td>
 *         <td>true</td>
 *         <td>true</td>
 *         <td>true</td>
 *     </tr>
 *     <tr>
 *         <td>SSLComp</td>
 *         <td>true</td>
 *         <td>false</td>
 *         <td>false</td>
 *     </tr>
 *     <tr>
 *         <td>SSLShareComp</td>
 *         <td>true</td>
 *         <td>true</td>
 *         <td>false</td>
 *     </tr>
 *     <tr>
 *         <td>NoSSL</td>
 *         <td>false</td>
 *         <td>false</td>
 *         <td>false</td>
 *     </tr>
 * </table>
 */
public enum JOSPSecurityLevel {

    // Values

    /**
     * Use an SSL encrypted connection with a unique instance id.
     * All certificates are validated using the default JKS.
     */
    SSLInstance,

    /**
     * Use an SSL encrypted connection with a unique instance id.
     * Certificates can be shared on the go.
     */
    SSLShareInstance,

    /**
     * Use an SSL encrypted connection with a shared instance id.
     * All certificates are validated using the default JKS.
     */
    SSLComp,

    /**
     * Use an SSL encrypted connection with a shared instance id.
     * Certificates can be shared on the go.
     */
    SSLShareComp,

    /**
     * Use a non-SSL connection.
     */
    NoSSL;


    // Determination methods

    /**
     * Calculate the security level based on the parameters.
     *
     * @param useSSL if the connection is encrypted
     * @param useShared if the remote certificate can be shared
     * @param useCertificatedId if the remote certificate contains peer's ID
     * @return the calculated security level
     * @throws IllegalArgumentException if the parameters are not valid
     */
    public static JOSPSecurityLevel calculate(boolean useSSL, boolean useShared, boolean useCertificatedId) {
        if (useSSL && useShared && useCertificatedId) return SSLShareInstance;
        if (useSSL && useShared) return SSLShareComp;
        if (useSSL && useCertificatedId) return SSLInstance;
        if (useSSL) return SSLComp;
        if (useShared && useCertificatedId) throw new IllegalArgumentException("Can't use shared without SSL.");
        if (useShared) throw new IllegalArgumentException("Can't use shared without SSL.");
        if (useCertificatedId) throw new IllegalArgumentException("Can't use certificated without SSL.");
        return NoSSL;
    }

    /**
     * Try to calculate the security level based on the parameters.
     *
     * @param useSSL if the connection is encrypted
     * @param useShared if the remote certificate can be shared
     * @param useCertificatedId if the remote certificate contains peer's ID
     * @return the calculated security level, or the `NoSSL` level if the
     * parameters are not valid
     */
    public static JOSPSecurityLevel tryCalculate(boolean useSSL, boolean useShared, boolean useCertificatedId) {
        return tryCalculate(useSSL, useShared, useCertificatedId, NoSSL);
    }

    /**
     * Try to calculate the security level based on the parameters. If fails,
     * it returns the given default level.
     *
     * @param useSSL if the connection is encrypted
     * @param useShared if the remote certificate can be shared
     * @param useCertificatedId if the remote certificate contains peer's ID
     * @param defaultLevel the default level to return if the parameters are not valid
     * @return the calculated security level, or the default level if the parameters are not valid
     */
    public static JOSPSecurityLevel tryCalculate(boolean useSSL, boolean useShared, boolean useCertificatedId, JOSPSecurityLevel defaultLevel) {
        try {
            return calculate(useSSL, useShared, useCertificatedId);
        } catch (IllegalArgumentException e) {
            return defaultLevel;
        }
    }

}
