package io.cldf.tool.services;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Singleton;

import io.cldf.api.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CLDFService {

  public CLDFArchive read(File file) throws IOException {
    return CLDF.read(file);
  }

  public void write(CLDFArchive archive, File file, boolean prettyPrint) throws IOException {
    CLDFWriter writer = new CLDFWriter(prettyPrint);
    writer.write(archive, file);
  }
}
