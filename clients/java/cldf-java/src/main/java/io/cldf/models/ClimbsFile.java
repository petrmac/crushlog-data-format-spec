package io.cldf.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Container for ClimbsFile data in CLDF archive. */
public class ClimbsFile {
  private List<Climb> climbs;
}
