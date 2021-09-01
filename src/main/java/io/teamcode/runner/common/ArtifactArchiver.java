package io.teamcode.runner.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teamcode.runner.common.util.FileUtils;
import io.teamcode.runner.network.model.Artifact;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Ant Pattern 을 지원하도록 하자.
 *
 * Created by chiang on 2017. 7. 16..
 */
public abstract class ArtifactArchiver {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactArchiver.class);

    private static final int  BUFFER_SIZE = 4096;

    /**
     * 압축 파일을 전달받은 디렉터리에서 해제합니다.
     *
     * @param outputDir
     * @param zipFile
     */
    public static final void unzip(final File outputDir, final File zipFile) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();

                if( entry.isDirectory() ) {
                    mkdirs(outputDir, name);
                    continue;
                }
                dir = dirpart(name);
                if( dir != null )
                    mkdirs(outputDir,dir);

                extractFile(zin, outputDir, name);
            }
        } finally {
        }
    }

    private static void mkdirs(File outputDir, String path)  {
        File d = new File(outputDir, path);
        if( !d.exists() )
            d.mkdirs();
    }

    private static String dirpart(String name)
    {
        int s = name.lastIndexOf( File.separatorChar );
        return s == -1 ? null : name.substring( 0, s );
    }

    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir,name)));
        int count = -1;
        while ((count = in.read(buffer)) != -1)
            out.write(buffer, 0, count);
        out.close();
    }

    public static final void zip(final File baseDir, final File zipFile, final List<String> filePaths) throws IOException {
        byte[] buffer = new byte[1024];

        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        FileInputStream in = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            File file;
            for(String filePath : filePaths){
                file = new File(filePath);

                ZipEntry ze = new ZipEntry(FileUtils.getRelativePath(baseDir, file.getAbsolutePath()));

                zos.putNextEntry(ze);

                in = new FileInputStream(file);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
            }

            zos.closeEntry();
        } catch(IOException e) {

            throw e;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(zos);
        }
    }

    public static final List<String> paths(File parentDir, Artifact artifact) throws FileNotFoundException {
        List<String> paths = new ArrayList<>();

        for (String path: artifact.getPaths()) {
            resolvePath(parentDir, path, paths);
        }

        return paths;
    }

    private static final void resolvePath(File parentDir, String path, List<String> paths) throws FileNotFoundException {
        File artifactFile = new File(parentDir, path);

        if (!artifactFile.exists()) {
            throw new FileNotFoundException(String.format("파일을 찾을 수 없어 빌드 결과를 압축할 수 없습니다. 다음 경로를 파이프라인 설정 파일에서 확인해 주세요. --> %s", path));
        }

        if (artifactFile.isFile()) {
            paths.add(artifactFile.getAbsolutePath());
            return;
        }

        File[] files = artifactFile.listFiles();
        if (files == null)
            return;

        for (File file: files) {
            if (file.isFile()) {
                paths.add(file.getAbsolutePath());
            }
            else {
                resolveDirectory(file, paths);
            }
        }

    }

    private static final void resolveDirectory(File directory, List<String> paths) {
        File[] files = directory.listFiles();
        for (File file: files) {
            if (file.isFile()) {
                paths.add(file.getAbsolutePath());
            }
            else {
                resolveDirectory(file, paths);
            }
        }
    }
}
