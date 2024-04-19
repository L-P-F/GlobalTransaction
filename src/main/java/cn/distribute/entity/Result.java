package cn.distribute.entity;

import lombok.Data;

@Data
public class Result<T>
{

    /**
     * 业务上的成功或失败
     */
    private boolean success = true;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回泛型数据，自定义类型
     */
    private T content;

    public Result()
    {
    }

    public static <T> Result<T> success()
    {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        return result;
    }
    public static <T> Result<T> success(T content)
    {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setContent(content);
        return result;
    }

    public static <T> Result<T> error(T message)
    {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(message.toString());
        return result;
    }

    public Result(T content)
    {
        this.content = content;
    }


    public void setSuccess(boolean success)
    {
        this.success = success;
    }


    public void setMessage(String message)
    {
        this.message = message;
    }

    public void setContent(T content)
    {
        this.content = content;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("CommonResp{");
        sb.append("success=").append(success);
        sb.append(", message='").append(message).append('\'');
        sb.append(", content=").append(content);
        sb.append('}');
        return sb.toString();
    }
}
