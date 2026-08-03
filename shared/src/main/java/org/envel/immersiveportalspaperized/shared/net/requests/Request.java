package org.envel.immersiveportalspaperized.shared.net.requests;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all cross-server request payloads.
 */
@Getter
@Setter
public abstract class Request implements Serializable {
   private static final long serialVersionUID = 1L;
   private int id;
}
