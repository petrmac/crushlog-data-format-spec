package app.crushlog.cldf.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Container for SessionsFile data in CLDF archive. */
public class SessionsFile {
  private List<Session> sessions;
}
