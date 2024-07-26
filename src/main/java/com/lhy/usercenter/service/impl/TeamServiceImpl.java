package com.lhy.usercenter.service.impl;

import ch.qos.logback.classic.joran.action.InsertFromJNDIAction;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lhy.usercenter.common.ErrorCode;
import com.lhy.usercenter.exception.BusinessException;
import com.lhy.usercenter.mapper.UserMapper;
import com.lhy.usercenter.mapper.UserTeamMapper;
import com.lhy.usercenter.model.Team;
import com.lhy.usercenter.model.User;
import com.lhy.usercenter.model.UserTeam;
import com.lhy.usercenter.model.dto.TeamJoinRequest;
import com.lhy.usercenter.model.dto.TeamQuery;
import com.lhy.usercenter.model.dto.TeamQuitRequest;
import com.lhy.usercenter.model.dto.TeamUpdateRequest;
import com.lhy.usercenter.model.enums.TeamStatusEnum;
import com.lhy.usercenter.model.vo.TeamUserVo;
import com.lhy.usercenter.model.vo.UserVo;
import com.lhy.usercenter.service.TeamService;
import com.lhy.usercenter.mapper.TeamMapper;
import com.lhy.usercenter.service.UserService;
import com.lhy.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.RedissonLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author ser_chsh
* @description 针对表【team】的数据库操作Service实现
* @createDate 2024-07-17 15:09:01
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private UserTeamService userTeamService;

    @Autowired
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        if (ObjectUtil.isNull(team)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"team参数为空");
        }
        if (ObjectUtil.isNull(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        int maxNum= Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum<1||maxNum>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数错误");
        }
        if (StringUtils.isBlank(team.getName())||team.getName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍标题过长");
        }
        if (StringUtils.isBlank(team.getDescription())||team.getDescription().length()>512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍描述过长");
        }
        int teamStatus=Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum teamStatusEnum=TeamStatusEnum.getTeamStatusEnum(teamStatus);
        if (teamStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍状态错误");
        }
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if (StringUtils.isBlank(team.getPassword())||team.getPassword().length()>32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码格式错误");
            }
        }
        LocalDateTime expireTime = team.getExpireTime();
        if (LocalDateTime.now().isAfter(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"超时时间设置错误");
        }

        QueryWrapper<Team> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",team.getUserId());
        Long teamNum = teamMapper.selectCount(queryWrapper);
        if (teamNum>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该用户创建队伍超过5个");
        }

        team.setId(null);
        team.setUserId(loginUser.getUserId());
        boolean save = this.save(team);
        if (!save||team.getId()==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"插入队伍失败");
        }

        UserTeam userTeam=new UserTeam();
        userTeam.setUserId(loginUser.getUserId());
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(LocalDateTime.now());
        boolean result = userTeamService.save(userTeam);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建队伍失败");
        }
        return team.getId();

    }

    @Override
    public List<TeamUserVo> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtil.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("max_num", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("user_id", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnum(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expire_time", new Date()).or().isNull("expire_time"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVo> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVo teamUserVO = new TeamUserVo();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVo userVO = new UserVo();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (ObjectUtil.isNull(teamUpdateRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if (teamUpdateRequest.getId()==null||teamUpdateRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = teamMapper.selectById(teamUpdateRequest.getId());
        if (ObjectUtil.isNull(oldTeam)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要修改的队伍不存在");
        }

        if (!loginUser.getUserId().equals(oldTeam.getUserId())&&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        Team newTeam = BeanUtil.copyProperties(teamUpdateRequest, Team.class);
        int i = teamMapper.updateById(newTeam);
        return i>0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (ObjectUtil.isNull(teamJoinRequest)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = teamMapper.selectById(teamJoinRequest.getTeamId());
        LocalDateTime expireTime = team.getExpireTime();
        if (ObjectUtil.isNull(team)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        //最多只能加入5个队伍
        long joinedTeamNum = userTeamService.count(new QueryWrapper<UserTeam>().eq("user_id", loginUser.getUserId()));
        if (joinedTeamNum>5){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"最多加入5个队伍");
        }
        //只能加入未满，未过期队伍
        if (expireTime!=null&&expireTime.isBefore(LocalDateTime.now())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"队伍已过期");
        }
        //不能加入私有队伍
        TeamStatusEnum teamStatusEnum=TeamStatusEnum.getTeamStatusEnum(team.getStatus());
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不允许加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)&&!team.getPassword().equals(password)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"加入队伍密码错误");
        }

        RLock rLock=redissonClient.getLock("yupao:join_team");
        try {
            while (true){
                if (rLock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                    System.out.println("getLock:"+Thread.currentThread().getId());
                    //不能重复加入已加入的队伍
                    long hasUserJoinTeam = userTeamService.count(new QueryWrapper<UserTeam>()
                            .eq("user_id", loginUser.getUserId()).eq("team_id", teamJoinRequest.getTeamId()));
                    if (hasUserJoinTeam>0){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能加入已经加入的队伍");
                    }
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamJoinRequest.getTeamId());
                    if (teamHasJoinNum>=team.getMaxNum()){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
                    }

                    UserTeam userTeam=new UserTeam();
                    userTeam.setUserId(loginUser.getUserId());
                    userTeam.setTeamId(teamJoinRequest.getTeamId());
                    userTeam.setJoinTime(LocalDateTime.now());
                    boolean result = userTeamService.save(userTeam);
                    return result;
                }
            }
        }catch (Exception e){
            log.error("cache error",e);
            return false;
        }finally {
            if (rLock.isHeldByCurrentThread()){
                System.out.println("unlock:"+Thread.currentThread().getId());
                rLock.unlock();
            }
        }





    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        Long teamId = teamQuitRequest.getTeamId();
        Team team = teamMapper.selectById(teamId);
        Long userId = loginUser.getUserId();
        if (ObjectUtil.isNull(team)){//判断队伍是否存在
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要退出的队伍不存在");
        }
        //判断自己是否已经加入了该队伍
        long count = userTeamService.count(new QueryWrapper<UserTeam>().eq("user_id", userId).eq("team_id", teamId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        //如果队伍只剩一人，退出直接解散队伍,否则进行退队逻辑操作
        long teamUserCount = userTeamService.count(new QueryWrapper<UserTeam>().eq("team_id", teamId));
        if (teamUserCount == 1) {
            teamMapper.deleteById(teamId);
        }else {
            if (team.getUserId().equals(userId)){//判断操作用户是否为队伍创建人
                //在用户队伍关系表中根据要退出的队伍id查询id，teamid，userid列并根据id进行升序排列   SELECT id,team_id,user_id FROM user_team WHERE is_delete=0 AND (team_id = ?) ORDER BY id ASC
                QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<UserTeam>().select("id", "team_id", "user_id").eq("team_id", teamId).orderByAsc("id");
                List<UserTeam> userTeamlist = userTeamService.list(queryWrapper);
                if (CollectionUtil.isEmpty(userTeamlist)||userTeamlist.size()<=1){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR);
                }
                UserTeam userTeam = userTeamlist.get(1);//获取列表中的第二名用户
                Long newLeaderId = userTeam.getUserId();

                Team newTeam=new Team();
                newTeam.setId(teamId);//根据id更新，所以需要设置id
                newTeam.setUserId(newLeaderId);//创建者（队长）退出后将队长设置为该队伍第二位加入的成员
                boolean result = this.updateById(newTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"退出队伍失败");
                }
            }
        }
        return userTeamService.remove(new QueryWrapper<UserTeam>().eq("team_id",teamId).eq("user_id",userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(Long teamId,User loginUser) {
        Team team = teamMapper.selectById(teamId);
        Long userId = loginUser.getUserId();
        //校验队伍是否存在
        if (ObjectUtil.isNull(team)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean isAdmin = userService.isAdmin(loginUser);
        if (!team.getUserId().equals(userId)&& !isAdmin){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        boolean result = userTeamService.remove(new QueryWrapper<UserTeam>().eq("team_id", team.getId()));
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍失败");
        }
        return this.removeById(teamId);


    }

    @Override
    public List<TeamUserVo> myCreateTeam(TeamQuery teamQuery, boolean b) {

        LambdaQueryWrapper<Team> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(StringUtils.isNotBlank(teamQuery.getSearchText()),Team::getName,teamQuery.getSearchText())
                .eq(teamQuery.getUserId()!=null&&teamQuery.getUserId()>0,Team::getUserId,teamQuery.getUserId());
        List<Team> list = this.list(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), lambdaQueryWrapper);
        List<TeamUserVo> teamUserVoList=new ArrayList<>();
        for (Team team : list) {
            TeamUserVo teamUserVo = BeanUtil.copyProperties(team, TeamUserVo.class);
            teamUserVoList.add(teamUserVo);
        }

        return teamUserVoList;
    }

    @Override
    public List<TeamUserVo> myJoinTeam(TeamQuery teamQuery, boolean b) {
//        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
//        queryWrapper.select("id","team_id","user_id","join_time").eq(teamQuery.getUserId()!=null&&teamQuery.getUserId()>0,"user_id",teamQuery.getUserId());
//        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        LambdaQueryWrapper<Team> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(StringUtils.isNotBlank(teamQuery.getSearchText()),Team::getName,teamQuery.getSearchText())
                .in(CollectionUtil.isNotEmpty(teamQuery.getIdList()),Team::getId,teamQuery.getIdList());

        List<Team> list = this.list(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), lambdaQueryWrapper);
        List<TeamUserVo> teamUserVoList=new ArrayList<>();
        for (Team team : list) {
            TeamUserVo teamUserVo = BeanUtil.copyProperties(team, TeamUserVo.class);
            teamUserVoList.add(teamUserVo);
        }

        return teamUserVoList;

    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("team_id", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }
}




