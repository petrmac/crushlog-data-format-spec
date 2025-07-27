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
/** Container for RoutesFile data in CLDF archive. */
public class RoutesFile {
  private List<Route> routes;
}
