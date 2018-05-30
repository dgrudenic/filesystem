package com.google.enterprise.adaptor.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class EgnayteAclAttributes {

  private String path;
  private String assignedGroups;
  private String assignedUsers;

  public EgnayteAclAttributes(String path, String assignedGroups, String assignedUsers) {
    this.path = path;
    this.assignedGroups = assignedGroups;
    this.assignedUsers = assignedUsers;
  }

  public Path getPath() {
    return Paths.get(path);
  }

  public String[] getAssignedGroups() {
    return assignedGroups != null ? assignedGroups.split(";") : null;
  }

  public String[] getAssignedUsers() {
    return assignedUsers != null ? assignedUsers.split(";") : null;
  }
}
