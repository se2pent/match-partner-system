package com.lhy.usercenter.service;

import com.lhy.usercenter.model.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.model.dto.TeamJoinRequest;
import com.lhy.usercenter.model.dto.TeamQuery;
import com.lhy.usercenter.model.dto.TeamQuitRequest;
import com.lhy.usercenter.model.dto.TeamUpdateRequest;
import com.lhy.usercenter.model.vo.TeamUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author ser_chsh
* @description 针对表【team】的数据库操作Service
* @createDate 2024-07-17 15:09:01
*/
public interface TeamService extends IService<Team> {
    /**
     * 增加队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    List<TeamUserVo> listTeams(TeamQuery teamQuery, boolean isAdmin);

    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(Long teamId,User loginUser);

    List<TeamUserVo> myCreateTeam(TeamQuery teamQuery, boolean b);

    List<TeamUserVo> myJoinTeam(TeamQuery teamQuery, boolean b);
}
