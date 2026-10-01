package com.treasure.restart.func.moment;

import com.treasure.restart.dto.MomentPublishRequest;
import com.treasure.restart.helper.UserContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moments")
public class MomentController {
    private MomentService momentService;
    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @PostMapping("/publish")
    public Object publish(@RequestBody MomentPublishRequest momentPublishRequest) {
        Long monmentId = momentService.publish(UserContext.getUserId(), momentPublishRequest);
        return monmentId;
    }

    @GetMapping("/list")
    public Object getMomentList(
            @RequestParam(defaultValue = "1")Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return momentService.getMomentList(page, size);
    }
}
