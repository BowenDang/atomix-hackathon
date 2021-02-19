package org.byc.atomix.service.controllers;

import org.byc.atomix.service.data.ServiceNode;
import org.byc.atomix.service.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class ServiceViewController {

  @Autowired
  private DemoService demoService;

  @GetMapping
  public String getMainPage() {
    return "";
  }

  @GetMapping("status")
  @ResponseBody
  public ServiceNode getNodeStatus() {

    return demoService.getStatus();
  }

  @PostMapping("start")
  @ResponseBody
  public String start() {

    demoService.startPublish();

    return "started";
  }

  @PostMapping("restart")
  @ResponseBody
  public String reset() {

    demoService.restart();

    return "restarted";
  }
}
