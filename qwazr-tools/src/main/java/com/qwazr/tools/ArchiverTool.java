/**
 * Copyright 2014-2015 Emmanuel Keller / QWAZR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.tools;


import com.qwazr.utils.IOUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class ArchiverTool extends AbstractTool {

	private static final Logger logger = LoggerFactory
			.getLogger(ArchiverTool.class);

	public enum CodecType {
		deflate, gzip, bzip2, z;
	}

	public CodecType codec;

	@Override
	public void load(File parentDir) {

	}

	@Override
	public void unload() {

	}


	public File dest_file(File source, String newExtension) {
		String newName = FilenameUtils.getBaseName(source.getName()) + '.' + newExtension;
		return new File(source.getParent(), newName);
	}

	private InputStream getCompressorNewInputStream(InputStream is) throws IOException, CompressorException {
		if (codec == null)
			return new CompressorStreamFactory().createCompressorInputStream(is);
		switch (codec) {
			case deflate:
				return new DeflateCompressorInputStream(is);
			case gzip:
				return new GZIPInputStream(is);
			case bzip2:
				return new BZip2CompressorInputStream(is);
			case z:
				return new ZCompressorInputStream(is);
			default:
				return new CompressorStreamFactory().createCompressorInputStream(is);
		}
	}

	public void decompress(File source, File destFile) throws IOException, CompressorException {
		if (destFile.exists())
			throw new IOException("The file already exists: " + destFile.getPath());
		InputStream input = getCompressorNewInputStream(new BufferedInputStream(new FileInputStream(source)));
		try {
			IOUtils.copy(input, destFile);
		} catch (IOException e) {
			throw new IOException("Unable to decompress the file: " + source.getPath(), e);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	public void decompress_dir(File sourceDir, String sourceExtension, File destDir, String destExtension)
			throws IOException, CompressorException {
		if (!sourceDir.exists())
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.getPath());
		if (!destDir.exists())
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.getPath());
		File[] sourceFiles = sourceDir.listFiles();
		if (sourceFiles == null)
			return;
		for (File sourceFile : sourceFiles) {
			if (!sourceFile.isFile())
				continue;
			String ext = FilenameUtils.getExtension(sourceFile.getName());
			if (!sourceExtension.equals(ext))
				continue;
			String newName = FilenameUtils.getBaseName(sourceFile.getName()) + '.' + destExtension;
			File destFile = new File(destDir, newName);
			if (destFile.exists())
				continue;
			decompress(sourceFile, destFile);
		}
	}

	public void decompress_dir(String sourcePath, String sourceExtension, String destPath, String destExtension)
			throws IOException, CompressorException {
		decompress_dir(new File(sourcePath), sourceExtension, new File(destPath), destExtension);
	}

	public void extract(File sourceFile, File destDir)
			throws IOException, ArchiveException {
		final InputStream is = new BufferedInputStream(new FileInputStream(sourceFile));
		try {
			ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(is);
			try {
				ArchiveEntry entry;
				for (; ; ) {
					entry = in.getNextEntry();
					if (entry == null)
						break;
					if (!in.canReadEntryData(entry))
						continue;
					if (entry.isDirectory()) {
						new File(destDir, entry.getName()).mkdir();
						continue;
					}
					File destFile = new File(destDir, entry.getName());
					if (!destFile.getParentFile().exists())
						destFile.getParentFile().mkdirs();
					IOUtils.copy(in, destFile);
				}
			} catch (IOException e) {
				throw new IOException("Unable to extract the archive: " + sourceFile.getPath(), e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		} catch (ArchiveException e) {
			throw new ArchiveException("Unable to extract the archive: " + sourceFile.getPath(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public void extract_dir(File sourceDir, String sourceExtension, File destDir, Boolean logErrorAndContinue)
			throws IOException, ArchiveException {
		if (logErrorAndContinue == null)
			logErrorAndContinue = false;
		if (!sourceDir.exists())
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.getPath());
		if (!destDir.exists())
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.getPath());
		File[] sourceFiles = sourceDir.listFiles();
		if (sourceFiles == null)
			return;
		for (File sourceFile : sourceFiles) {
			if (!sourceFile.isFile())
				continue;
			String ext = FilenameUtils.getExtension(sourceFile.getName());
			if (!sourceExtension.equals(ext))
				continue;
			try {
				extract(sourceFile, destDir);
			} catch (IOException | ArchiveException e) {
				if (logErrorAndContinue)
					logger.error(e.getMessage(), e);
				else
					throw e;
			}
		}
	}

	public void extract_dir(String sourcePath, String sourceExtension, String destPath, Boolean logErrorAndContinue)
			throws IOException, ArchiveException {
		extract_dir(new File(sourcePath), sourceExtension, new File(destPath), logErrorAndContinue);
	}

}
