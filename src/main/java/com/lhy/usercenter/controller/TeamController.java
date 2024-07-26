package com.lhy.usercenter.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lhy.usercenter.common.BaseResponse;
import com.lhy.usercenter.common.ErrorCode;
import com.lhy.usercenter.common.ResultUtil;
import com.lhy.usercenter.contant.UserContant;
import com.lhy.usercenter.exception.BusinessException;
import com.lhy.usercenter.model.Team;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.model.UserTeam;
import com.lhy.usercenter.model.dto.*;
import com.lhy.usercenter.model.login.UserLoginRequest;
import com.lhy.usercenter.model.request.UserRegisterRequest;
import com.lhy.usercenter.model.vo.TeamUserVo;
import com.lhy.usercenter.service.TeamService;
import com.lhy.usercenter.service.UserService;
import com.lhy.usercenter.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:5173/", allowCredentials = "true")
@RequestMapping("/team")
public class TeamController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserTeamService userTeamService;


    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody Team team,HttpServletRequest request){
        if (ObjectUtil.isNull(team)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
//        LocalDateTime expireTime = team.getExpireTime();
//        ZonedDateTime zonedDateTime=expireTime.atZone(ZoneId.of("UTC"));
//        ZonedDateTime localdatetime = zonedDateTime.withZoneSameInstant(ZoneId.of("GMT+8"));
//        team.setExpireTime(localdatetime.toLocalDateTime());
        User loginUser = userService.getLoginUser(request);
        long teamId = teamService.addTeam(team,loginUser);

        return ResultUtil.success(teamId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request){
        if (ObjectUtil.isNull(teamUpdateRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        return ResultUtil.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (ObjectUtil.isNull(deleteRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(deleteRequest.getTeamId(), loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtil.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(Long teamId){
        if (teamId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (ObjectUtil.isNull(team)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtil.success(team);
    }


    @GetMapping("/list")
    public BaseResponse<List<TeamUserVo>> getTeamList(TeamQuery teamQuery, HttpServletRequest request){
        if (ObjectUtil.isNull(teamQuery)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVo> teamList = teamService.listTeams(teamQuery,isAdmin);
        final List<Long> teamIdList = teamList.stream().map(TeamUserVo::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
        try {
        User loginUser = userService.getLoginUser(request);
        queryWrapper.eq("user_id",loginUser.getUserId());
        queryWrapper.in("team_id",teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team->{
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        }catch (Exception e){

        }
        QueryWrapper<UserTeam> userTeamJoinQueryMapper=new QueryWrapper<>();
        userTeamJoinQueryMapper.in("team_id",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryMapper);
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team->team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new ArrayList<>()).size()));
        return ResultUtil.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> getTeamListByPage(TeamQuery teamQuery){
        if (ObjectUtil.isNull(teamQuery)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        Team team = BeanUtil.copyProperties(teamQuery, Team.class);
        Page<Team> page=new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper=new QueryWrapper<>(team);
        Page<Team> teamListByPage = teamService.page(page,queryWrapper);
        return ResultUtil.success(teamListByPage);
    }

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if (ObjectUtil.isNull(teamJoinRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtil.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest,HttpServletRequest request){
        if (ObjectUtil.isNull(teamQuitRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result=teamService.quitTeam(teamQuitRequest,loginUser);
        return ResultUtil.success(result);

    }

    /**
     * 查询我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVo>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (ObjectUtil.isNull(teamQuery)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        teamQuery.setStatus(null);
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getUserId());
        List<TeamUserVo> teamUserVoList = teamService.myCreateTeam(teamQuery, true);
        return ResultUtil.success(teamUserVoList);
    }

    /**
     * 查询我加入的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVo>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (ObjectUtil.isNull(teamQuery)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getUserId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList=new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVo> teamUserVoList = teamService.myJoinTeam(teamQuery, true);
        return ResultUtil.success(teamUserVoList);
    }







}
