package org.envel.immersiveportalspaperized.bukkit.net.requests;

import lombok.Getter;
import lombok.Setter;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

@Getter
@Setter
public class TestForwardedRequest extends Request {
   private static final long serialVersionUID = 1L;
   private IntVector testField;
}
