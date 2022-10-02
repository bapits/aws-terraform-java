package com.bapits.labs.sample.aws.terraform.model;

public enum FileProcessStatus {
  COLLECTED, // file received
  PROCESSED, // file is processed 
  ALREADY_PROCESSED, // file has been already processed
  TRANSFERRED, // file has been deleted
  ERROR
}
