package org.ninng.businesssvc.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalListItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 174591972073189310L;

    private String id;
    private String name;
    private String title;
    private int sort;
}
