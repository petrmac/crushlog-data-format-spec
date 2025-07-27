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
/** Container for SectorsFile data in CLDF archive. */
public class SectorsFile {
  private List<Sector> sectors;
}
