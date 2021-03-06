package minispring.beans.factory.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author lihua
 * @since 2021/8/26
 */
@Data
@RequiredArgsConstructor
public class BeanReference {

    private final String beanName;
}
