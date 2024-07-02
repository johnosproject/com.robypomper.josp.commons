/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2024 Roberto Pompermaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.robypomper.java;

import com.robypomper.comm.trustmanagers.AbsCustomTrustManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;


/**
 * Utils class to manage Java Key Stores (JKS).
 * <p>
 * This class provide static methods to manage Java Key Stores (JKS) and their
 * certificates. You can:
 * <ul>
 *     <li>load, store or even generate Key Stores to files</li>
 *     <li>extract certificates from JKS and export to files</li>
 *     <li>load certificates from files, bytes or JKS</li>
 *     <li>extract certificate's id from certificate</li>
 *     <li>copy certificates from TrustManager to KeyStore and vice versa</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class JavaJKS {

    // Class constants

    public static final String KEYSTORE_TYPE = "PKCS12";        // KeyStore.getDefaultType() => jks
    public static final String CERT_TYPE = "X.509";
    public static final String SIGING_ALG = "RSA";
    public static final int KEY_SIZE = 2048;
    public static final int CERT_VALIDITY_DAYS = 3650;


    // JKS load, store, generate

    /**
     * Generate new Java Key Store containing the Key Pair and the corresponding
     * Certificate Chain.
     * <p>
     * The certificate chain contain only the Key Pair certificate with given id.
     * <p>
     * This method is deprecated, use {@link #generateNewKeyStoreFile(String, String, String, String)}
     * instead.
     *
     * @param certificateID the string used as certificate commonName (this will
     *                      be visible also to other peer side).
     * @param ksPass        the JKS password.
     * @param certAlias     the alias associated to the generated certificate.
     * @return a valid Key Store with Key Pair and Certificate Chain.
     */
    @Deprecated
    public static KeyStore generateKeyStore(String certificateID, String ksPass, String certAlias) throws GenerationException {
        if (certificateID == null || certificateID.isEmpty())
            throw new IllegalArgumentException("Error on generating keystore, certificateID can't be null or empty");
        if (ksPass == null || ksPass.length() < 6)
            throw new IllegalArgumentException(String.format("Error on generating keystore for '%s', password must contains at least 6 chars", certificateID));
        if (certAlias == null || certAlias.isEmpty())
            throw new IllegalArgumentException(String.format("Error on generating keystore for '%s', certAlias can't be null or empty", certificateID));

        File tmpKeyStoreFile;
        try {
            tmpKeyStoreFile = File.createTempFile("tmpks", "");
            if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
                tmpKeyStoreFile.delete();
        } catch (IOException e) {
            throw new GenerationException(String.format("Error on generating keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), null);
        }

        KeyStore ks;
        try {
            String genKeyStoreDir = Paths.get(System.getProperty("java.home"), "bin/keytool").toString();
            if (System.getProperty("os.name").startsWith("Windows"))
                genKeyStoreDir = JavaFiles.toWindowsPath(genKeyStoreDir);
            String genKeyStore = genKeyStoreDir + String.format(" -genkey -noprompt -keyalg %s -keysize %d -validity %d -alias %s -dname 'CN=%s,OU=com.robypomper.comm,O=John,L=Trento,S=TN,C=IT' -keystore %s -deststoretype pkcs12 -storepass '%s' -keypass '%s'", SIGING_ALG, KEY_SIZE, CERT_VALIDITY_DAYS, certAlias, certificateID, tmpKeyStoreFile.getAbsolutePath(), ksPass, ksPass);
            String output = JavaExecProcess.execCmd(genKeyStore, JavaExecProcess.DEF_TIMEOUT * 5).trim();
            if (!tmpKeyStoreFile.exists()) {
                throw new GenerationException(String.format("Error on generating keystore for '%s' commonName, temporary keystore not create because '%s'", certificateID, output));
            }

            ks = loadKeyStore(tmpKeyStoreFile.getAbsolutePath(), ksPass);

        } catch (IOException | LoadingException |
                 JavaExecProcess.ExecStillAliveException e) {
            if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
                tmpKeyStoreFile.delete();
            throw new GenerationException(String.format("Error on generating keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), e);
        }

        if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
            tmpKeyStoreFile.delete();
        return ks;
    }

    /**
     * Load {@link KeyStore} from given path and use given password.
     *
     * @param ksPath the file path containing the KeyStore to load.
     * @param ksPass the string containing the KeyStore password.
     * @return the loaded KeyStore.
     * @throws LoadingException if an error occurs during loading process.
     */
    public static KeyStore loadKeyStore(String ksPath, String ksPass) throws LoadingException {
        if (ksPath == null || ksPath.isEmpty())
            throw new IllegalArgumentException("Error on loading keystore, ksPath can't be null or empty");

        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
            FileInputStream keyStoreInputStream = new FileInputStream(ksPath);
            ks.load(keyStoreInputStream, ksPass != null && !ksPass.isEmpty() ? ksPass.toCharArray() : null);
            return ks;

        } catch (Throwable e) {
            throw new LoadingException(String.format("Error loading key store from '%s' file because %s", ksPath, e.getMessage()), e);
        }
    }

    /**
     * Save given KeyStore to given file path and use given password.
     *
     * @param ks     the KeyStore to store.
     * @param ksPath the file path where to store the KeyStore.
     * @param ksPass the string containing the KeyStore password.
     * @throws StoreException if an error occurs during storing process.
     */
    public static void storeKeyStore(KeyStore ks, String ksPath, String ksPass) throws StoreException {
        //log.trace(String.format("Storing KeyStore on '%s' file", ksPath));

        try {
            dirExistOrCreate(ksPath);
            FileOutputStream keyStoreOutputStream = new FileOutputStream(ksPath);
            //OutputStream keyStoreOutputStream = Files.newOutputStream(Paths.get(ksPath));
            ks.store(keyStoreOutputStream, ksPass.toCharArray());
        } catch (Exception e) {
            throw new StoreException(String.format("Error storing key store from '%s' file because %s", ksPath, e.getMessage()), e);
        }
    }

    /**
     * Generate new JKS file containing a new certificate with given id and alias.
     * <p>
     * In order to supply the missing of Cripthography Extension, the keystore
     * and his certificate will be generated using the `keytool` cmd.
     *
     * @param certificateID the string used as certificate commonName (this will
     *                      be visible also to other peer side).
     * @param ksPath        the JKS password.
     * @param ksPass        the JKS password.
     * @param certAlias     the alias associated to the generated certificate.
     * @return the loaded KeyStore.
     * @throws GenerationException if an error occurs during generation or
     *                             loading process.
     */
    public static KeyStore generateAndLoadNewKeyStoreFile(String certificateID, String ksPath, String ksPass, String certAlias) throws GenerationException {
        generateNewKeyStoreFile(certificateID, ksPath, ksPass, certAlias);
        try {
            return loadKeyStore(ksPath, ksPass);
        } catch (LoadingException e) {
            throw new GenerationException(e.getMessage(), e);
        }
    }

    /**
     * Generate new temporary JKS file containing a new certificate with given
     * id and alias. The JKS file will be deleted on exit.
     * <p>
     * In order to supply the missing of Cripthography Extension, the keystore
     * and his certificate will be generated using the `keytool` cmd.
     *
     * @param certificateID the string used as certificate commonName (this will
     *                      be visible also to other peer side).
     * @param ksPass        the JKS password.
     * @param certAlias     the alias associated to the generated certificate.
     * @return the loaded KeyStore.
     * @throws GenerationException if an error occurs during generation or
     *                             loading process.
     */
    public static KeyStore generateAndLoadNewKeyStoreTempFile(String certificateID, String ksPass, String certAlias) throws GenerationException {
        File tempFile;
        try {
            tempFile = File.createTempFile("tempFile", ".txt");
        } catch (IOException e) {
            throw new GenerationException(String.format("Error on generating keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), e);
        }
        tempFile.deleteOnExit();
        return generateAndLoadNewKeyStoreFile(certificateID, tempFile.getPath(), ksPass, certAlias);
    }

    /**
     * Generate new JKS file containing a new certificate with given id and alias.
     * <p>
     * In order to supply the missing of Cripthography Extension, the keystore
     * and his certificate will be generated using the `keytool` cmd.
     *
     * @param certificateID the string used as certificate commonName (this will
     *                      be visible also to other peer side).
     * @param ksPass        the JKS password.
     * @param certAlias     the alias associated to the generated certificate.
     * @throws GenerationException if an error occurs during generation process.
     */
    public static void generateNewKeyStoreFile(String certificateID, String ksPath, String ksPass, String certAlias) throws GenerationException {
        if (certificateID == null || certificateID.isEmpty())
            throw new IllegalArgumentException("Error on generating keystore, certificateID can't be null or empty");
        if (ksPath == null || ksPath.isEmpty())
            throw new IllegalArgumentException("Error on generating keystore, ksPath can't be null or empty");
        if (ksPass == null || ksPass.length() < 6)
            throw new IllegalArgumentException(String.format("Error on generating keystore for '%s', password must contains at least 6 chars", certificateID));
        if (certAlias == null || certAlias.isEmpty())
            throw new IllegalArgumentException(String.format("Error on generating keystore for '%s', certAlias can't be null or empty", certificateID));

        File tmpKeyStoreFile;
        try {
            tmpKeyStoreFile = File.createTempFile("tmpks", "");
            if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
                tmpKeyStoreFile.delete();
        } catch (IOException e) {
            throw new GenerationException(String.format("Error on generating keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), null);
        }

        try {
            String genKeyStoreDir = Paths.get(System.getProperty("java.home"), "bin/keytool").toString();
            if (System.getProperty("os.name").startsWith("Windows"))
                genKeyStoreDir = JavaFiles.toWindowsPath(genKeyStoreDir);
            String genKeyStore = genKeyStoreDir + String.format(" -genkey -noprompt -keyalg %s -keysize %d -validity %d -alias %s -dname 'CN=%s,OU=com.robypomper.comm,O=John,L=Trento,S=TN,C=IT' -keystore %s -deststoretype pkcs12 -storepass '%s' -keypass '%s'",
                    SIGING_ALG, KEY_SIZE, CERT_VALIDITY_DAYS, certAlias, certificateID, tmpKeyStoreFile.getAbsolutePath(), ksPass, ksPass);
            String output = JavaExecProcess.execCmd(genKeyStore, JavaExecProcess.DEF_TIMEOUT * 5).trim();
            if (!tmpKeyStoreFile.exists())
                throw new GenerationException(String.format("Error on generating keystore for '%s' commonName, temporary keystore not create because '%s'", certificateID, output));

        } catch (IOException | JavaExecProcess.ExecStillAliveException e) {
            if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
                tmpKeyStoreFile.delete();
            throw new GenerationException(String.format("Error on generating keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), e);
        }

        try {
            Files.copy(tmpKeyStoreFile.toPath(), Paths.get(ksPath));
        } catch (IOException e) {
            if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
                tmpKeyStoreFile.delete();
            throw new GenerationException(String.format("Error on coping keystore for '%s' commonName because [%s] %s", certificateID, e.getClass().getSimpleName(), e.getMessage()), null);
        }

        if (tmpKeyStoreFile.exists()) //noinspection ResultOfMethodCallIgnored
            tmpKeyStoreFile.delete();
    }


    // JKS's certificates

    /**
     * Save <code>ksAlias</code> certificate contained in <code>ksFile</code>
     * to <code>exportCertFile</code>given certificate chain as public certificate to the KeyStore.
     * <p>
     * This method is deprecated, use {@link #extractKeyStoreCertificate(KeyStore, String)} instead.
     *
     * @param keyStore the KeyStore containing the certificate to export.
     * @param ksAlias  the alias of the certificate to export.
     * @return the certificate extracted from the KeyStore.
     * @throws GenerationException if an error occurs during reading JKS.
     */
    @Deprecated
    public static Certificate extractCertificate(KeyStore keyStore, String ksAlias) throws GenerationException {
        try {
            return extractKeyStoreCertificate(keyStore, ksAlias);
        } catch (LoadingException e) {
            throw new GenerationException(e.getMessage(), e);
        }
    }

    /**
     * Save <code>ksAlias</code> certificate contained in <code>ksFile</code>
     * to <code>exportCertFile</code>given certificate chain as public certificate to the KeyStore.
     *
     * @param keyStore the KeyStore containing the certificate to export.
     * @param ksAlias  the alias of the certificate to export.
     * @return the certificate extracted from the KeyStore.
     * @throws LoadingException if an error occurs during reading JKS.
     */
    public static Certificate extractKeyStoreCertificate(KeyStore keyStore, String ksAlias) throws LoadingException {
        //log.trace(String.format("Extracting certificate '%s'", ksAlias));

        try {
            Certificate cert = keyStore.getCertificate(ksAlias);
            if (cert == null)
                throw new LoadingException(String.format("Can't find certificate for alias '%s'", ksAlias));
            return cert;

        } catch (KeyStoreException e) {
            throw new LoadingException(String.format("Error extracting certificate because %s", e.getMessage()), e);
        }
    }

    /**
     * Save <code>ksAlias</code> certificate contained in <code>ksFile</code>
     * to <code>exportCertFile</code>given certificate chain as public certificate to the KeyStore.
     * <p>
     * This method is deprecated, use {@link #exportKeyStoreCertificateToFile(KeyStore, String, String)} instead.
     *
     * @param keyStore       the KeyStore containing the certificate to export.
     * @param exportCertFile the file path where to store the certificate.
     * @param ksAlias        the alias of the certificate to export.
     * @throws GenerationException if an error occurs during reading JKS or
     *                             during exporting to file process.
     */
    @Deprecated
    public static void exportCertificate(KeyStore keyStore, String exportCertFile, String ksAlias) throws GenerationException {
        try {
            exportKeyStoreCertificateToFile(keyStore, exportCertFile, ksAlias);
        } catch (LoadingException | StoreException e) {
            throw new GenerationException(e.getMessage(), e);
        }
    }

    /**
     * Save <code>ksAlias</code> certificate contained in <code>ksFile</code>
     * to <code>exportCertFile</code>given certificate chain as public certificate to the KeyStore.
     *
     * @param keyStore       the KeyStore containing the certificate to export.
     * @param exportCertFile the file path where to store the certificate.
     * @param ksAlias        the alias of the certificate to export.
     * @throws LoadingException if an error occurs during reading JKS.
     * @throws StoreException   if an error occurs during exporting to file process.
     */
    public static void exportKeyStoreCertificateToFile(KeyStore keyStore, String exportCertFile, String ksAlias) throws LoadingException, StoreException {
        //log.trace(String.format("Exporting certificate '%s' on '%s' file", ksAlias, exportCertFile));

        try {
            Certificate cert = extractKeyStoreCertificate(keyStore, ksAlias);
            byte[] buf = cert.getEncoded();

            dirExistOrCreate(exportCertFile);
            FileOutputStream os = new FileOutputStream(exportCertFile);
            os.write(buf);
            os.close();

        } catch (IOException | CertificateEncodingException e) {
            throw new StoreException(String.format("Error exporting certificate to file %s because %s", exportCertFile, e.getMessage()), e);
        }
    }


    // Certificate loading

    /**
     * Load the Certificate from given file. The certificate must be stored in
     * X.509 format (binary).
     *
     * @param file the file containing the certificate.
     * @return the loaded certificate.
     * @throws LoadingException if an error occurs during loading process.
     */
    public static Certificate loadCertificateFromFile(File file) throws LoadingException {
        //log.trace(String.format("Loading certificate from '%s' file", file.getPath()));

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return loadCertificateFromBytes(fileBytes);

        } catch (Exception e) {
            throw new LoadingException(String.format("Error loading certificate from bytes because %s", e.getMessage()), e);
        }
    }

    /**
     * Load the Certificate from given array of bytes.
     * <p>
     * This method expect the certificate to be stored in X.509 format (binary).
     *
     * @param bytesCert the byte array containing the certificate.
     * @return the loaded certificate.
     * @throws LoadingException if an error occurs during loading process.
     */
    public static Certificate loadCertificateFromBytes(byte[] bytesCert) throws LoadingException {
        //log.trace("Loading certificate from bytes");

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance(CERT_TYPE);
            InputStream inputStream = new ByteArrayInputStream(bytesCert);
            return certFactory.generateCertificate(inputStream);

        } catch (Exception e) {
            throw new LoadingException(String.format("Error loading certificate from bytes because %s", e.getMessage()), e);
        }
    }

    /**
     * Load the Certificate from given KeyStore with specified alias.
     *
     * @param ksPath  the file path containing the KeyStore to load.
     * @param ksPass  the string containing the KeyStore password.
     * @param ksAlias the alias of the certificate to load.
     * @return the loaded certificate.
     * @throws LoadingException if an error occurs during loading process.
     */
    public static Certificate loadCertificateFromKeyStoreFile(String ksPath, String ksPass, String ksAlias) throws LoadingException {
        KeyStore keyStore = loadKeyStore(ksPath, ksPass);
        return extractKeyStoreCertificate(keyStore, ksAlias);
    }


    // Certificate methods


    /**
     * This method extract the certificate's id from the certificate's DN.
     *
     * @param localCertificate the certificate from which extract the id. It
     *                         must be an instance of X509CertImpl.
     * @return the certificate's id from given certificate.
     */
    public static String getCertificateId(Certificate localCertificate) {
        if (!(localCertificate instanceof X509Certificate))
            throw new IllegalArgumentException("Certificate must be an instance of X509CertImpl");

        return JavaSSL.extractCN(localCertificate);
    }


    // Certificate copy (KeyStore 2 TrustManager)

    /**
     * Copy all certificates from given {@link javax.net.ssl.TrustManager} to given
     * {@link KeyStore}.
     * <p>
     * It copies all certificates without duplicates. Certificates are duplicated
     * if their alias is already present in the destination.
     *
     * @param keyStore     the certificates source.
     * @param trustManager the certificates' destination.
     * @throws StoreException if an error occurs during copying process.
     */
    public static void copyCertsFromTrustManagerToKeyStore(KeyStore keyStore, AbsCustomTrustManager trustManager) throws StoreException {
        //log.trace("Synchronizing certificate from trust manager to key store");

        for (Map.Entry<String, Certificate> aliasAndCert : trustManager.getCertificates().entrySet())
            try {
                keyStore.setCertificateEntry(aliasAndCert.getKey(), aliasAndCert.getValue());
            } catch (KeyStoreException e) {
                if (!e.getMessage().startsWith("Cannot overwrite own certificate"))
                    throw new StoreException(String.format("Error coping certificate from trust manager to key store because %s", e.getMessage()), e);
            }
    }

    /**
     * Copy all certificates from given {@link KeyStore} to given
     * {@link javax.net.ssl.TrustManager}.
     * <p>
     * It copies all certificates without duplicates. Certificates are duplicated
     * if their alias is already present in the destination.
     *
     * @param keyStore     the certificates' destination.
     * @param trustManager the certificates source.
     * @throws LoadingException if an error occurs during copying process.
     * @throws StoreException   if an error occurs during copying process.
     */
    public static void copyCertsFromKeyStoreToTrustManager(KeyStore keyStore, AbsCustomTrustManager trustManager) throws LoadingException, StoreException {
        //log.trace("Synchronizing certificate from key store to trust manager");

        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(alias);
                if (cert == null) {
                    //log.error(String.format("Can't find certificate for alias '%s'", alias));
                    continue;
                }
                trustManager.addCertificate(alias, cert);
            }

        } catch (KeyStoreException e) {
            throw new LoadingException(String.format("Error coping certificate from key store to trust manager because %s", e.getMessage()), e);
        } catch (AbsCustomTrustManager.UpdateException e) {
            throw new StoreException(String.format("Error coping certificate from key store to trust manager because %s", e.getMessage()), e);
        }
    }


    // Utils methods

    /**
     * Create directory for given path.
     * <p>
     * The path can define a directory or a file. If it represent a file, then
     * this method will create the parent directory.
     *
     * @param path string containing the path to create.
     */
    private static void dirExistOrCreate(String path) throws IOException {
        File f = new File(path).getAbsoluteFile();
        File dir = f.isDirectory() ? f : f.getParentFile();
        if (!dir.exists())
            if (!dir.mkdirs())
                throw new IOException(String.format("Can't create directory for path %s", path));

    }


    // Exception

    /**
     * Exceptions thrown on errors during JKS generation processes.
     */
    public static class JKSException extends Throwable {
        public JKSException(String msg) {
            super(msg);
        }

        public JKSException(String msg, Throwable e) {
            super(msg, e);
        }
    }

    /**
     * Exceptions thrown on errors during JKS generation processes.
     */
    public static class GenerationException extends JKSException {
        public GenerationException(String msg) {
            super(msg);
        }

        public GenerationException(String msg, Throwable e) {
            super(msg, e);
        }
    }

    /**
     * Exceptions thrown on errors during JKS loading processes.
     */
    public static class LoadingException extends JKSException {
        public LoadingException(String msg) {
            super(msg);
        }

        public LoadingException(String msg, Throwable e) {
            super(msg, e);
        }
    }

    /**
     * Exceptions thrown on errors during JKS storing processes.
     */
    public static class StoreException extends JKSException {
        public StoreException(String msg, Throwable e) {
            super(msg, e);
        }
    }

}
